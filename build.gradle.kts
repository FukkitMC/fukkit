import net.fabricmc.loom.task.GenerateSourcesTask

plugins {
    id("fabric-loom") version "0.9.16"
    id("io.github.juuxel.loom-quiltflower") version "1.1.1"
    id("xyz.fukkit.crusty") version "2.0.0"
    id("uk.jamierocks.propatcher") version "2.0.0"
}

group = "xyz.fukkit"
version = "1.0.0-SNAPSHOT"

repositories {
    maven {
        name = "JitPack"
        url = uri("https://jitpack.io/")
    }
}

dependencies {
    minecraft("net.minecraft", "minecraft", "1.17")
    mappings(fukkit.mappings())

    modImplementation("net.fabricmc", "fabric-loader", "0.11.6")
    // modImplementation("net.fabricmc.fabric-api", "fabric-api", "0.36.0+1.17")
    modImplementation("com.github.Chocohead", "Fabric-ASM", "v2.3")

    compileOnly("com.google.code.findbugs", "jsr305", "3.0.2")
}

sourceSets {
    main {
        java.srcDir("src/main/craftbukkit")
        java.srcDir("src/main/minecraft")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

loom {
    accessWidener = file("src/main/resources/fukkit.aw")
}

patches {
    rootDir = file(".gradle/sources-1.17")
    target = file("src/main/minecraft")
    patches = file("patches")
}

val classesToPatch = file("patches/classlist.txt").readLines()
    .map { it.trim() }
    .map {
        val index = it.indexOf('#')

        if (index == -1) {
            it
        } else {
            it.substring(index)
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

    resetSources {
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
}
