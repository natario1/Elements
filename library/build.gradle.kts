import com.otaliastudios.tools.publisher.PublisherExtension.License
import com.otaliastudios.tools.publisher.PublisherExtension.Release

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("maven-publisher-bintray")
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

    dataBinding.isEnabled = true
}

dependencies {
    val kotlinVersion = property("kotlinVersion") as String
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    api("androidx.recyclerview:recyclerview:1.1.0")
    api("androidx.lifecycle:lifecycle-livedata:2.2.0")
    api("com.jakewharton.timber:timber:4.7.1")
}

publisher {
    auth.user = "BINTRAY_USER"
    auth.key = "BINTRAY_KEY"
    auth.repo = "BINTRAY_REPO"
    project.artifact = "elements"
    project.description = "A modular approach to RecyclerView adapters with reusable, testable, independent, coordinated components."
    project.group = "com.otaliastudios"
    project.url = "https://github.com/natario1/Elements"
    project.addLicense(License.APACHE_2_0)
    release.setSources(Release.SOURCES_AUTO)
    release.setDocs(Release.DOCS_AUTO)
}



