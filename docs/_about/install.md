---
layout: page
title: "Install"
description: "Integrate in your project"
order: 1
---

The library works on API 14+, which is the only requirement and should be met by most projects nowadays.

It is publicly hosted on [JCenter](https://bintray.com/natario/android/Elements), where you
can download the AAR package. To fetch with Gradle, make sure you add the JCenter repository in your root projects `build.gradle` file:

```groovy
allprojects {
  repositories {
    jcenter()
  }
}
```

Then simply download the latest version:

```groovy
implementation 'com.otaliastudios:elements:{{ site.github_version }}'
```

No other configuration steps are needed.