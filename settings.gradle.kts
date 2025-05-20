//pluginManagement {
//    repositories {
//        google()
//        gradlePluginPortal()
//        mavenCentral()
//    }
//
//    plugins {
//        id("com.android.application") version "8.2.2" apply false
//        id("org.jetbrains.kotlin.android") version "1.9.22" apply false
//        id("com.google.gms.google-services") version "4.4.0" apply false
//    }
//}
//
//dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
//    repositories {
//        google()
//        mavenCentral()
//    }
//}
//
//rootProject.name = "MapaEksploracji"
//include(":app")
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("com.android.application") version "8.2.2" apply false
        id("org.jetbrains.kotlin.android") version "1.9.22" apply false
        id("com.google.gms.google-services") version "4.4.0" apply false
    }
}

//dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
//    repositories {
//        google()
//        mavenCentral()
//        maven {
//            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
//            credentials {
//                username = "mapbox"
//                password = System.getenv("MAPBOX_DOWNLOADS_TOKEN") ?: "<pk.eyJ1Ijoic2ltb3hrc3kiLCJhIjoiY21hd3hwcnEwMGduZDJqc2U5N3QzczJlbiJ9.wBoenJhdDAtikyW9g3q8mw>"
//            }
//        }
//    }
//}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = "pk.eyJ1Ijoic2ltb3hrc3kiLCJhIjoiY21hd3hwcnEwMGduZDJqc2U5N3QzczJlbiJ9.wBoenJhdDAtikyW9g3q8mw"
            }
        }
    }
}


rootProject.name = "MapaEksploracji"
include(":app")
