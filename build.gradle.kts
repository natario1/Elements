buildscript {

    extra["minSdkVersion"] = 14
    extra["compileSdkVersion"] = 30
    extra["targetSdkVersion"] = 30

    repositories {
        mavenCentral()
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.20")
        classpath("com.otaliastudios.tools:publisher:0.3.3")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        jcenter()
    }
}

tasks.register("clean", Delete::class) {
    delete(buildDir)
}
