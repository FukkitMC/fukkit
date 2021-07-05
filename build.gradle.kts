import net.fabricmc.loom.configuration.processors.JarProcessor
import net.fabricmc.loom.task.GenerateSourcesTask
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
    id("fabric-loom") version "0.9.16"
    id("io.github.juuxel.loom-quiltflower") version "1.1.1"
    id("xyz.fukkit.crusty") version "2.0.0"
    id("uk.jamierocks2.propatcher") version "2.0.0" apply false
}

group = "xyz.fukkit"
version = "1.0.0-SNAPSHOT"

dependencies {
    minecraft("net.minecraft", "minecraft", "1.17")
    mappings(fukkit.mappings())

    modImplementation("net.fabricmc", "fabric-loader", "0.11.6")
    // modImplementation("net.fabricmc.fabric-api", "fabric-api", "0.36.0+1.17")

    compileOnly("com.google.code.findbugs", "jsr305", "3.0.2")
}

sourceSets {
    main {
        java.srcDir("src/main/craftbukkit")
        java.srcDir("src/main/minecraft")
    }

    create("vanilla") {
        compileClasspath += main.get().compileClasspath
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

val classesToPatch = file("patches/classlist.txt").readLines()
    .map {
        val s = it.trim()
        val index = s.indexOf('#')

        if (index == -1) {
            s
        } else {
            s.substring(index)
        }
    }
    .filter { it.isNotEmpty() }

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

            val decompile = project.tasks["genSourcesWithQuiltflower"] as GenerateSourcesTask
            val decompiled = run {
                val method =
                    GenerateSourcesTask::class.java.getDeclaredMethod("getMappedJarFileWithSuffix", String::class.java)
                method.isAccessible = true
                method.invoke(decompile, "-sources.jar") as File
            }

            if (!decompiled.exists()) {
                decompile.doTask()
            }

            copy {
                from(zipTree(decompiled))
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
