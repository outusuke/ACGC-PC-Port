plugins {
    id("com.android.application")
}

val sdlDir = file("../third_party/SDL2-2.32.10")
if (!sdlDir.exists()) {
    throw GradleException("SDL2 sources missing, run android/fetch-sdl.sh first")
}

val generatedAssets = layout.buildDirectory.dir("generated/assets")
val generatedRes = layout.buildDirectory.dir("generated/launcher-res")

val copyShaders by tasks.registering(Copy::class) {
    from("../../pc/shaders")
    into(generatedAssets.map { it.dir("shaders") })
}

val copyIcon by tasks.registering(Copy::class) {
    from("../../pc/res/icon.png")
    rename { "ic_launcher.png" }
    into(generatedRes.map { it.dir("mipmap-xxxhdpi") })
}

tasks.named("preBuild") {
    dependsOn(copyShaders, copyIcon)
}

android {
    namespace = "io.github.outusuke.acgcpcport"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "io.github.outusuke.acgcpcport"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.9.3"

        ndk {
            abiFilters += "arm64-v8a"
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DSDL2_SOURCE_DIR=${sdlDir.absolutePath}",
                    "-DANDROID_STL=c++_static"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("../../pc/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets["main"].apply {
        java.srcDir("${sdlDir}/android-project/app/src/main/java")
        assets.srcDir(generatedAssets)
        res.srcDir(generatedRes)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
