 $RepoPath = "C:\Users\E1\Documents\GitHub\ai-video-generator"
# إذا كان مسار المستودع مختلفاً، عدله في السطر أعلاه

 $app = "$RepoPath\app\src\main"
 $java = "$app\java\com\wathiq\aivideo"
 $res = "$app\res"

# 1. إنشاء المجلدات
 $dirs = @("$app", "$java", "$java\ui", "$java\service", "$java\util", "$res\layout", "$res\values", "$res\drawable", "$res\mipmap-anydpi-v26", "$RepoPath\gradle\wrapper", "$RepoPath\.github\workflows")
foreach ($d in $dirs) { New-Item -ItemType Directory -Path $d -Force | Out-Null }

# دالة الكتابة بترميز نظيف
function W([string]$p, [string]$c) { 
    $e = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($p, $c, $e) 
}

# 2. ملفات الإعداد
W "$RepoPath\settings.gradle" @"
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "AI Video Generator"
include ':app'
"@

W "$RepoPath\build.gradle" @"
plugins {
    id 'com.android.application' version '8.1.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.0' apply false
}
"@

W "$RepoPath\gradle.properties" @"
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.daemon=true
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
"@

W "$RepoPath\.gitignore" @"
.gradle/
build/
/local.properties
*.iml
.idea/
.DS_Store
/captures/
.cxx/
*.jks
*.keystore
*.log
Thumbs.db
"@

W "$RepoPath\app\build.gradle" @"
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}
android {
    namespace 'com.wathiq.aivideo'
    compileSdk 34
    defaultConfig {
        applicationId "com.wathiq.aivideo"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = '17' }
    buildFeatures { viewBinding true }
}
dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
"@

W "$RepoPath\gradle\wrapper\gradle-wrapper.properties" @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.3-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@

Write-Host "Part 1 Done: Config files created" -ForegroundColor Green