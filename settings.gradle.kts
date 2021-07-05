rootProject.name = "fukkit"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()

        maven {
            name = "FukkitMC"
            url = uri("https://raw.githubusercontent.com/FukkitMC/fukkit-repo/master")

            content {
                includeGroup("xyz.fukkit")
                includeGroup("xyz.fukkit.crusty")
                includeGroup("io.github.fukkitmc")
            }
        }

        maven {
            name = "Cotton"
            url = uri("https://server.bbkr.space/artifactory/libs-release/")

            content {
                includeGroup("io.github.juuxel.loom-quiltflower")
                includeGroup("io.github.juuxel")
            }
        }

        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
    }
}
