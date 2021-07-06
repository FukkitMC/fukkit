plugins {
    id("crusty-loom") version "0.10.2"
    id("xyz.fukkit.crusty") version "2.3.7"
    id("uk.jamierocks2.propatcher") version "2.0.0"
}

group = "xyz.fukkit"
version = "1.0.0-SNAPSHOT"

val buildData = crusty.latestBuildData
loom.setBuildData(buildData)

repositories {
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
}

val priority = configurations.create("priority")
val mainSourceSet = sourceSets.main.get()
mainSourceSet.compileClasspath = priority + mainSourceSet.compileClasspath

val patched = sourceSets.create("patched") {
    java.srcDir(mainSourceSet.java.srcDirs)
}

dependencies {
    priority(patched.output)
    minecraft("net.minecraft", "minecraft", "1.17")
    mappings(crusty.getCrustyMappings(buildData, "net.fabricmc:intermediary:1.17:v2"))

    modImplementation("net.fabricmc", "fabric-loader", "0.11.6")
    // modImplementation("net.fabricmc.fabric-api", "fabric-api", "0.36.0+1.17")

    compileOnly("com.google.code.findbugs", "jsr305", "3.0.2")
    implementation("org.spigotmc", "spigot-api", "1.17-R0.1-SNAPSHOT")
    implementation("jline", "jline", "2.12.1")
    implementation("org.apache.logging.log4j", "log4j-iostreams", "2.14.1")

    testImplementation("junit", "junit", "4.13.1")
    testImplementation("org.hamcrest", "hamcrest-library", "1.3")
}

//tasks.classes.get().dependsOn(tasks.getByName("patchedClasses"))

val patchedCompileOnly = configurations.getByName(patched.compileOnlyConfigurationName)
patchedCompileOnly.extendsFrom(configurations.getByName(mainSourceSet.runtimeElementsConfigurationName))
patchedCompileOnly.extendsFrom(configurations.getByName(mainSourceSet.compileOnlyConfigurationName))
patchedCompileOnly.extendsFrom(configurations.getByName(net.fabricmc.loom.util.Constants.Configurations.MINECRAFT_NAMED))

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

loom {
    accessWidener = file("src/main/resources/fukkit.aw")
}

patches {
    rootDir = file(".gradle/sources-1.17").apply {
        mkdirs()
    }
    target = file("src/patched/java").apply {
        mkdirs()
    }
    patches = file("patches").apply {
        mkdirs()
    }
}

val classesToPatch = file("patches/classlist.txt").readLines()
    .map { it.substringBefore('#').trim() }
    .filter { it.isNotEmpty() }
    .toMutableSet()
val root = file("patches/")
root.walk().toList()
    .filter { it.isFile }
    .map { it.relativeTo(root) }
    .forEach { classesToPatch.add(it.toString().removeSuffix(".patch")) }

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

    resetSources {
        doFirst {
            delete {
                delete(rootDir)
            }

            val decompiled = crusty.getCrustySources(buildData)

            copy {
                from(decompiled)
                into(rootDir)

                classesToPatch.forEach(::include)
            }
        }
    }
}
