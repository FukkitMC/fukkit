rootProject.name = "fukkit"

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "Devan"
            url = uri("https://storage.googleapis.com/devan-maven/")
        }
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
}
