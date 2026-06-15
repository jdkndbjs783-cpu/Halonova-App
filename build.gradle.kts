// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

tasks.register<Zip>("packageProjectZip") {
    archiveFileName.set("hello-nova.zip")
    destinationDirectory.set(file(rootDir))

    doFirst {
        val debugApk = file("app/build/outputs/apk/debug/app-debug.apk")
        val releaseDir = file("app/build/outputs/apk/release")
        val releaseApk = file("app/build/outputs/apk/release/app-release.apk")
        if (debugApk.exists()) {
            releaseDir.mkdirs()
            debugApk.copyTo(releaseApk, overwrite = true)
        }
    }

    // 1. Root files
    from(rootDir) {
        include("build.gradle.kts")
        include("settings.gradle.kts")
        include("gradle.properties")
        include(".env.example")
        include(".gitignore")
        include("metadata.json")
    }

    // 2. Gradle folder
    from(file("${rootDir}/gradle")) {
        into("gradle")
    }

    // 3. App folder, excluding build files
    from(file("${rootDir}/app")) {
        into("app")
        exclude("build")
    }

    // 4. APK folder containing generated APK files
    from(file("${rootDir}/app/build/outputs/apk/debug")) {
        include("app-debug.apk")
        into("APK")
    }
    from(file("${rootDir}/app/build/outputs/apk/release")) {
        include("app-release.apk")
        into("APK")
    }
}

tasks.register("printVerificationReport") {
    doLast {
        val debugApk = file("app/build/outputs/apk/debug/app-debug.apk")
        val releaseApk = file("app/build/outputs/apk/release/app-release.apk")
        val zipFile = file("hello-nova.zip")

        println("--- VERIFICATION REPORT ---")
        if (debugApk.exists()) {
            val sizeKb = debugApk.length() / 1024
            println("DEBUG_APK_PATH: ${debugApk.absolutePath}")
            println("DEBUG_APK_SIZE: $sizeKb KB (${debugApk.length()} bytes)")
            val bytes = debugApk.readBytes()
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(bytes)
            val sha = hashBytes.joinToString("") { "%02x".format(it) }
            println("DEBUG_APK_SHA256: $sha")
        } else {
            println("DEBUG_APK_STATUS: MISSING")
        }

        if (releaseApk.exists()) {
            val sizeKb = releaseApk.length() / 1024
            println("RELEASE_APK_PATH: ${releaseApk.absolutePath}")
            println("RELEASE_APK_SIZE: $sizeKb KB (${releaseApk.length()} bytes)")
            val bytes = releaseApk.readBytes()
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(bytes)
            val sha = hashBytes.joinToString("") { "%02x".format(it) }
            println("RELEASE_APK_SHA256: $sha")
        } else {
            println("RELEASE_APK_STATUS: MISSING")
        }

        if (zipFile.exists()) {
            val sizeKb = zipFile.length() / 1024
            println("ZIP_PATH: ${zipFile.absolutePath}")
            println("ZIP_SIZE: $sizeKb KB (${zipFile.length()} bytes)")
        } else {
            println("ZIP_STATUS: MISSING")
        }
    }
}

