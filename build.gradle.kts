import net.fabricmc.loom.task.GenerateSourcesTask
import uk.jamierocks.propatcher.task.ResetSourcesTask

plugins {
    id("fabric-loom") version "0.9.16"
    id("io.github.juuxel.loom-quiltflower") version "1.1.1"
    id("xyz.fukkit.crusty") version "2.0.0"
    id("uk.jamierocks.propatcher") version "2.0.0"
}

group = "xyz.fukkit"
version = "1.0.0-SNAPSHOT"

sourceSets {
    main {
        java.srcDir("src/main/minecraft")
    }
}

dependencies {
    minecraft("net.minecraft", "minecraft", "1.17")
    mappings(fukkit.mappings())

    modImplementation("net.fabricmc", "fabric-loader", "0.11.6")
    modImplementation("net.fabricmc.fabric-api", "fabric-api", "0.36.0+1.17")

    compileOnly("com.google.code.findbugs", "jsr305", "3.0.2")
}

patches {
    rootDir = file(".gradle/sources-1.17")
    target = file("src/main/minecraft")
    patches = file("patches")
}

val GenerateSourcesTask.output: File
    get() {
        val method = GenerateSourcesTask::class.java.getDeclaredMethod("getMappedJarFileWithSuffix", String::class.java)
        method.isAccessible = true
        return method.invoke(this, "-sources.jar") as File
    }

val classes = file("patches/classlist.txt").readLines()
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

(tasks["resetSources"] as ResetSourcesTask).apply {
    doFirst {
        delete {
            delete(rootDir)
        }

        val decompile = tasks["genSourcesWithQuiltflower"] as GenerateSourcesTask
        val decompiled = decompile.output

        if (!decompiled.exists()) {
            decompile.doTask()
        }

        copy {
            from(zipTree(decompiled))
            into(rootDir)

            classes.forEach(::include)
        }
    }
}
