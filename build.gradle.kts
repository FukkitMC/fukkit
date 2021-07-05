import net.fabricmc.loom.configuration.processors.JarProcessor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.zeroturnaround.zip.ByteSource
import org.zeroturnaround.zip.ZipUtil
import uk.jamierocks.propatcher.task.ApplyPatchesTask
import uk.jamierocks.propatcher.task.MakePatchesTask
import uk.jamierocks.propatcher.task.ResetSourcesTask
import xyz.fukkit.ClassStripper
import xyz.fukkit.EnvironmentStrippingData

plugins {
    id("crusty-loom") version "0.10.1"
    id("xyz.fukkit.crusty") version "2.2.2"
    id("uk.jamierocks2.propatcher") version "2.0.0" apply false
}

group = "xyz.fukkit"
version = "1.0.0-SNAPSHOT"

val buildData = crusty.latestBuildData
loom.setBuildData(buildData)

repositories {
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}

dependencies {
    minecraft("net.minecraft", "minecraft", "1.17")
    mappings(fukkit.mappings())

    modImplementation("net.fabricmc", "fabric-loader", "0.11.6")
    // modImplementation("net.fabricmc.fabric-api", "fabric-api", "0.36.0+1.17")
    implementation("org.spigotmc:spigot-api:1.17-R0.1-SNAPSHOT")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation("jline:jline:2.12.1")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.14.1")
    implementation("org.ow2.asm:asm:9.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("mysql:mysql-connector-java:5.1.49")
    implementation("org.apache.maven:maven-resolver-provider:3.8.1")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.7.0")
    implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.7.0")
    implementation("junit:junit:4.13.1")
    implementation("org.hamcrest:hamcrest-library:1.3")

    compileOnly("com.google.code.findbugs", "jsr305", "3.0.2")
}

sourceSets {
    main {
        java.srcDir("src/main/craftbukkit")
        java.srcDir("src/main/minecraft")
    }

    create("vanilla") {
        compileClasspath += main.get().runtimeClasspath
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

loom {
    accessWidener = file("src/main/resources/fukkit.aw")
    addJarProcessor(SideStripperJarProcessor.SERVER)
}

val explicitClasses = file("patches/classlist.txt").readLines()
    .map { it.substringBefore('#').trim() }
    .filter { it.isNotEmpty() }
val classesToPatch = HashSet<String>(explicitClasses)
val root = file("patches/vanilla/")
root.walk().filter { it.isFile }.asIterable().map { it.relativeTo(root) }.forEach { classesToPatch.add(it.toString().replace(".patch", ".java")) }

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(16)
    }

    withType<AbstractArchiveTask> {
        from(rootProject.file("LICENSE"))
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    val resetVanillaSources by registering(ResetSourcesTask::class) {
        rootDir = file(".gradle/sources-1.17")
        target = file("src/vanilla/java")

        rootDir.mkdirs()
        target.mkdirs()

        doFirst {
            delete {
                delete(rootDir)
            }

            val decompiled = crusty.getCrustySources(buildData) // todo avoid thing

            copy {
                from(decompiled)
                into(rootDir)

                classesToPatch.forEach(::include)
            }
        }
    }

    val applyVanillaPatches by registering(ApplyPatchesTask::class) {
        dependsOn(resetVanillaSources)
        target = file("src/vanilla/java")
        patches = file("patches/vanilla")

        target.mkdirs()
        patches.mkdirs()
    }

    val makeVanillaPatches by registering(MakePatchesTask::class) {
        rootDir = file(".gradle/sources-1.17")
        target = file("src/vanilla/java")
        patches = file("patches/vanilla")

        rootDir.mkdirs()
        target.mkdirs()
        patches.mkdirs()
    }

    val resetCraftbukkitSources by registering(ResetSourcesTask::class) {
        dependsOn(applyVanillaPatches)
        rootDir = file("src/vanilla/java")
        target = file("src/main/minecraft")

        rootDir.mkdirs()
        target.mkdirs()
    }

    val applyCraftbukkitPatches by registering(ApplyPatchesTask::class) {
        dependsOn(resetCraftbukkitSources)
        target = file("src/main/minecraft")
        patches = file("patches/craftbukkit")

        target.mkdirs()
        patches.mkdirs()
    }

    val makeCraftbukkitPatches by registering(MakePatchesTask::class) {
        rootDir = file("src/vanilla/java")
        target = file("src/main/minecraft")
        patches = file("patches/craftbukkit")

        rootDir.mkdirs()
        target.mkdirs()
        patches.mkdirs()
    }
}

enum class SideStripperJarProcessor : JarProcessor {
    CLIENT, SERVER;

    override fun setup() {}

    override fun process(file: File?) {
        val toRemove = mutableSetOf<String>()
        val toTransform = mutableSetOf<ByteSource>()

        ZipUtil.iterate(file) { `in`, zipEntry ->
            val name: String = zipEntry.name
            if (!zipEntry.isDirectory && name.endsWith(".class")) {
                val original = ClassNode()
                ClassReader(`in`).accept(original, 0)
                val stripData =
                    EnvironmentStrippingData(Opcodes.ASM8, this.name)
                original.accept(stripData)
                if (stripData.stripEntireClass()) {
                    toRemove.add(name)
                } else if (!stripData.isEmpty) {
                    val classWriter = ClassWriter(0)
                    original.accept(
                        ClassStripper(
                            Opcodes.ASM8,
                            classWriter,
                            stripData.stripInterfaces,
                            stripData.stripFields,
                            stripData.stripMethods
                        )
                    )
                    toTransform.add(ByteSource(name, classWriter.toByteArray()))
                }
            }
        }

        ZipUtil.replaceEntries(file, toTransform.toTypedArray())
        ZipUtil.removeEntries(file, toRemove.toTypedArray())
        ZipUtil.addEntry(file, "side.txt", name.toByteArray())
    }

    override fun isInvalid(file: File?): Boolean {
        return !ZipUtil.unpackEntry(file, "side.txt").contentEquals(name.toByteArray())
    }
}
