plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

android {
    setCompileSdkVersion(rootProject.extra["compileSdkVersion"] as Int)

    defaultConfig {
        applicationId = "com.otaliastudios.elements.sample"
        setMinSdkVersion(rootProject.extra["minSdkVersion"] as Int)
        setTargetSdkVersion(rootProject.extra["targetSdkVersion"] as Int)
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
    }

    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
        getByName("test").java.srcDir("src/test/kotlin")
    }

    dataBinding.isEnabled = true
}

dependencies {
    val kotlinVersion = property("kotlinVersion") as String
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation(project(":library"))
}
