import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    kotlin("android")
    kotlin("kapt")
    id("kotlinx-serialization")
    id("com.android.application")
}

dependencies {
    compileOnly(project(":hideapi"))

    implementation(project(":core"))
    implementation(project(":service"))
    implementation(project(":design"))
    implementation(project(":common"))

    implementation(libs.kotlin.coroutine)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.coordinator)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    implementation(libs.androidx.activity.ktx)
    
    // Fix: Room dependency for Database access in MainActivity
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
}

tasks.getByName("clean", type = Delete::class) {
    delete(file("release"))
}

// GeoIP download task removed to reduce APK size (ZIVPN uses MATCH rule)

afterEvaluate {
    tasks.forEach {
        if (it.name.startsWith("assemble")) {
            // Task dependency removed
        }
    }
}

tasks.getByName("clean", type = Delete::class) {
    // Assets cleanup removed
}