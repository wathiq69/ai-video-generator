 $RepoPath = "C:\Users\E1\Documents\GitHub\ai-video-generator"
 $app = "$RepoPath\app\src\main"
 $java = "$app\java\com\wathiq\aivideo"
 $res = "$app\res"

function W([string]$p, [string]$c) { 
    $e = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($p, $c, $e) 
}

# 1. AndroidManifest.xml
W "$app\AndroidManifest.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <application
        android:name=".App"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AIVideo"
        android:requestLegacyExternalStorage="true"
        tools:targetApi="34">
        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AIVideo">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"@

# 2. colors.xml
W "$res\values\colors.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="bg_start">#0F0C29</color>
    <color name="bg_center">#5C4B8A</color>
    <color name="bg_end">#1A5F7A</color>
    <color name="card_bg">#1AFFFFFF</color>
    <color name="card_border">#40FFFFFF</color>
    <color name="white_text">#FFFFFF</color>
    <color name="primary_text">#FFFFFF</color>
    <color name="secondary_text">#B3FFFFFF</color>
    <color name="btn_top">#5BC8FF</color>
    <color name="btn_bottom">#1E90FF</color>
    <color name="btn_shadow">#0B3D91</color>
    <color name="accent_top">#FFB74D</color>
    <color name="accent_bottom">#FF6D00</color>
    <color name="accent_shadow">#E65100</color>
    <color name="success">#4CAF50</color>
    <color name="error">#F44336</color>
</resources>
"@

# 3. strings.xml
W "$res\values\strings.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AI Video Maker</string>
    <string name="app_subtitle">تحويل النص الى فيديو</string>
    <string name="hint_prompt">اكتب وصف الفيديو هنا بالعربي او الانجليزي...</string>
    <string name="btn_generate">توليد الفيديو</string>
    <string name="btn_select_image">اختيار صورة</string>
    <string name="btn_save">حفظ في الاستوديو</string>
    <string name="btn_share">مشاركة</string>
    <string name="portrait">عمودي</string>
    <string name="landscape">افقي</string>
    <string name="generating">جاري توليد الفيديو...</string>
    <string name="done">تم بنجاح!</string>
    <string name="error">حدث خطأ، حاول مرة اخرى</string>
    <string name="no_image">لم يتم اختيار صورة</string>
    <string name="image_selected">تم اختيار الصورة</string>
    <string name="saved">تم الحفظ في الاستوديو</string>
    <string name="permission_denied">تم رفض الاذن</string>
</resources>
"@

# 4. themes.xml
W "$res\values\themes.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.AIVideo" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/btn_bottom</item>
        <item name="colorPrimaryVariant">@color/btn_top</item>
        <item name="colorOnPrimary">@color/white_text</item>
        <item name="colorSecondary">@color/accent_bottom</item>
        <item name="colorSecondaryVariant">@color/accent_top</item>
        <item name="colorOnSecondary">@color/white_text</item>
        <item name="android:statusBarColor" tools:targetApi="l">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:windowBackground">@drawable/bg_gradient</item>
    </style>
    <style name="TitleText">
        <item name="android:textColor">@color/white_text</item>
        <item name="android:textSize">24sp</item>
        <item name="android:textStyle">bold</item>
    </style>
    <style name="SubtitleText">
        <item name="android:textColor">@color/secondary_text</item>
        <item name="android:textSize">14sp</item>
    </style>
    <style name="BodyText">
        <item name="android:textColor">@color/white_text</item>
        <item name="android:textSize">16sp</item>
    </style>
    <style name="ButtonPrimary" parent="Widget.MaterialComponents.Button">
        <item name="android:background">@drawable/btn_primary</item>
        <item name="android:textColor">@color/white_text</item>
        <item name="android:textSize">16sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:padding">16dp</item>
        <item name="android:elevation">6dp</item>
    </style>
    <style name="ButtonAccent" parent="Widget.MaterialComponents.Button">
        <item name="android:background">@drawable/btn_accent</item>
        <item name="android:textColor">@color/white_text</item>
        <item name="android:textSize">16sp</item>
        <item name="android:textStyle">bold</item>
        <item name="android:padding">16dp</item>
        <item name="android:elevation">6dp</item>
    </style>
</resources>
"@

# 5. bg_gradient.xml
W "$res\drawable\bg_gradient.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="135" android:startColor="@color/bg_start" android:centerColor="@color/bg_center" android:endColor="@color/bg_end" android:type="linear" />
</shape>
"@

# 6. btn_primary.xml
W "$res\drawable\btn_primary.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <layer-list>
            <item android:top="2dp"><shape><solid android:color="@color/btn_shadow" /><corners android:radius="20dp" /></shape></item>
            <item android:bottom="2dp" android:left="2dp" android:right="2dp" android:top="2dp"><shape><gradient android:angle="90" android:startColor="@color/btn_bottom" android:endColor="@color/btn_top" /><corners android:radius="20dp" /><stroke android:width="1dp" android:color="#60FFFFFF" /></shape></item>
        </layer-list>
    </item>
    <item>
        <layer-list>
            <item android:top="6dp"><shape><solid android:color="@color/btn_shadow" /><corners android:radius="20dp" /></shape></item>
            <item android:bottom="6dp" android:left="2dp" android:right="2dp" android:top="2dp"><shape><gradient android:angle="90" android:startColor="@color/btn_top" android:endColor="@color/btn_bottom" /><corners android:radius="20dp" /><stroke android:width="1dp" android:color="#60FFFFFF" /></shape></item>
        </layer-list>
    </item>
</selector>
"@

# 7. btn_accent.xml
W "$res\drawable\btn_accent.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <layer-list>
            <item android:top="2dp"><shape><solid android:color="@color/accent_shadow" /><corners android:radius="20dp" /></shape></item>
            <item android:bottom="2dp" android:left="2dp" android:right="2dp" android:top="2dp"><shape><gradient android:angle="90" android:startColor="@color/accent_bottom" android:endColor="@color/accent_top" /><corners android:radius="20dp" /><stroke android:width="1dp" android:color="#60FFFFFF" /></shape></item>
        </layer-list>
    </item>
    <item>
        <layer-list>
            <item android:top="6dp"><shape><solid android:color="@color/accent_shadow" /><corners android:radius="20dp" /></shape></item>
            <item android:bottom="6dp" android:left="2dp" android:right="2dp" android:top="2dp"><shape><gradient android:angle="90" android:startColor="@color/accent_top" android:endColor="@color/accent_bottom" /><corners android:radius="20dp" /><stroke android:width="1dp" android:color="#60FFFFFF" /></shape></item>
        </layer-list>
    </item>
</selector>
"@

# 8. ic_launcher_background.xml
W "$res\drawable\ic_launcher_background.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="135" android:startColor="@color/bg_start" android:endColor="@color/bg_center" />
</shape>
"@

# 9. ic_launcher_foreground.xml
W "$res\drawable\ic_launcher_foreground.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="M36,30 L72,30 L72,38 L80,38 L80,82 L36,82 Z" />
    <path android:fillColor="@color/btn_top" android:pathData="M40,42 L68,42 L68,78 L40,78 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M52,50 L60,58 L56,58 L56,70 L52,70 Z" />
</vector>
"@

# 10. Mipmap icons
W "$res\mipmap-anydpi-v26\ic_launcher.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"@

W "$res\mipmap-anydpi-v26\ic_launcher_round.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"@

# 11. activity_main.xml
W "$res\layout\activity_main.xml" @"
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@drawable/bg_gradient"
    android:fitsSystemWindows="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp"
        android:gravity="center">
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/app_name"
            style="@style/TitleText" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/app_subtitle"
            style="@style/SubtitleText"
            android:layout_marginTop="4dp" />
    </LinearLayout>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        android:paddingBottom="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="24dp"
                app:cardElevation="8dp"
                app:cardBackgroundColor="@color/card_bg"
                android:layout_marginBottom="16dp">
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="20dp">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="الوصف (Prompt)"
                        style="@style/BodyText"
                        android:layout_marginBottom="8dp" />
                    <EditText
                        android:id="@+id/etPrompt"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="@string/hint_prompt"
                        android:textColor="@color/white_text"
                        android:textColorHint="#80FFFFFF"
                        android:minLines="4"
                        android:gravity="top|start"
                        android:background="@android:color/transparent"
                        android:padding="12dp" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="24dp"
                app:cardElevation="8dp"
                app:cardBackgroundColor="@color/card_bg"
                android:layout_marginBottom="16dp">
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="20dp">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="الصورة (اختياري)"
                        style="@style/BodyText"
                        android:layout_marginBottom="8dp" />
                    <Button
                        android:id="@+id/btnSelectImage"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="@string/btn_select_image"
                        style="@style/ButtonAccent" />
                    <TextView
                        android:id="@+id/tvImageStatus"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/no_image"
                        style="@style/SubtitleText"
                        android:layout_marginTop="8dp" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="24dp"
                app:cardElevation="8dp"
                app:cardBackgroundColor="@color/card_bg"
                android:layout_marginBottom="16dp">
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="20dp">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="الاتجاه"
                        style="@style/BodyText"
                        android:layout_marginBottom="8dp" />
                    <RadioGroup
                        android:id="@+id/rgOrientation"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal">
                        <RadioButton
                            android:id="@+id/rbPortrait"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="@string/portrait"
                            android:textColor="@color/white_text"
                            android:checked="true" />
                        <RadioButton
                            android:id="@+id/rbLandscape"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="@string/landscape"
                            android:textColor="@color/white_text" />
                    </RadioGroup>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <Button
                android:id="@+id/btnGenerate"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/btn_generate"
                style="@style/ButtonPrimary"
                android:layout_marginBottom="16dp" />

            <ProgressBar
                android:id="@+id/progressBar"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:visibility="gone"
                android:layout_marginBottom="16dp" />

            <TextView
                android:id="@+id/tvStatus"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=""
                style="@style/SubtitleText"
                android:layout_gravity="center"
                android:layout_marginBottom="16dp" />

            <VideoView
                android:id="@+id/videoView"
                android:layout_width="match_parent"
                android:layout_height="250dp"
                android:visibility="gone"
                android:layout_marginBottom="16dp" />

            <LinearLayout
                android:id="@+id/layoutButtons"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:visibility="gone">
                <Button
                    android:id="@+id/btnSave"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/btn_save"
                    style="@style/ButtonAccent"
                    android:layout_marginEnd="8dp" />
                <Button
                    android:id="@+id/btnShare"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/btn_share"
                    style="@style/ButtonPrimary"
                    android:layout_marginStart="8dp" />
            </LinearLayout>

        </LinearLayout>
    </ScrollView>
</LinearLayout>
"@

# 12. GitHub Actions Workflow
W "$RepoPath\.github\workflows\build-apk.yml" @"
name: Build APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Setup Gradle 8.3
      uses: gradle/gradle-build-action@v3
      with:
        gradle-version: '8.3'
    - name: Download gradle-wrapper.jar
      run: |
        mkdir -p gradle/wrapper
        curl -sL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.3.0/gradle/wrapper/gradle-wrapper.jar
    - name: Build Debug APK
      run: gradle assembleDebug --no-daemon --stacktrace
    - name: Upload Debug APK
      uses: actions/upload-artifact@v4
      with:
        name: AIVideoMaker-APK
        path: app/build/outputs/apk/debug/*.apk
        retention-days: 90
"@

Write-Host "Part 2 Done: Manifest, Resources, Layout, Workflow created" -ForegroundColor Green