rootProject.name = "fukkit"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()

        maven {
            name = "Devan"
            url = uri("https://storage.googleapis.com/devan-maven/")

            content {
                includeGroup("xyz.fukkit")
                includeGroup("xyz.fukkit.crusty")
                includeGroup("io.github.fukkitmc")
                includeGroup("uk.jamierocks2")
                includeGroup("uk.jamierocks2.propatcher")
            }
        }

        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
    }
}
