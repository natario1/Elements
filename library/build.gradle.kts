import com.otaliastudios.tools.publisher.common.License
import com.otaliastudios.tools.publisher.common.Release

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("com.otaliastudios.tools.publisher")
}

android {
    setCompileSdkVersion(property("compileSdkVersion") as Int)

    defaultConfig {
        setMinSdkVersion(property("minSdkVersion") as Int)
        setTargetSdkVersion(property("targetSdkVersion") as Int)
        versionName = "0.3.7"
    }

    sourceSets {
        get("main").java.srcDirs("src/main/kotlin")
        get("test").java.srcDirs("src/test/kotlin")
    }

    buildFeatures {
        dataBinding = true
    }

    kotlinOptions {
        // Until the explicitApi() works in the Kotlin block...
        // https://youtrack.jetbrains.com/issue/KT-37652
        freeCompilerArgs += listOf("-Xexplicit-api=strict")
    }
}

dependencies {
    api("androidx.recyclerview:recyclerview:1.1.0")
    api("androidx.lifecycle:lifecycle-livedata:2.2.0")
    api("com.jakewharton.timber:timber:4.7.1")
}

publisher {
    project.artifact = "elements"
    project.description = "A modular approach to RecyclerView adapters with reusable, testable, independent, coordinated components."
    project.group = "com.otaliastudios"
    project.url = "https://github.com/natario1/Elements"
    project.addLicense(License.APACHE_2_0)
    release.setSources(Release.SOURCES_AUTO)
    release.setDocs(Release.DOCS_AUTO)
    bintray {
        auth.user = "BINTRAY_USER"
        auth.key = "BINTRAY_KEY"
        auth.repo = "BINTRAY_REPO"
    }
    directory {
        directory = "build/maven"
    }
}



