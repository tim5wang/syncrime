import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

// 读取并递增 versionCode (基于日期 + 当日构建次数)
fun generateVersionCode(projectDir: File): Int {
    val date = Date()
    val dateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())
    val datePrefix = dateFormat.format(date).toInt()
    
    // 读取构建次数文件
    val buildCountFile = File(projectDir, "build_count.txt")
    val lastBuildDateFile = File(projectDir, "last_build_date.txt")
    
    var buildCount = 0
    var lastDate = ""
    
    if (lastBuildDateFile.exists()) {
        lastDate = lastBuildDateFile.readText().trim()
    }
    
    val today = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
    
    if (lastDate == today && buildCountFile.exists()) {
        buildCount = buildCountFile.readText().trim().toIntOrNull() ?: 0
        buildCount++
    } else {
        buildCount = 1
    }
    
    // 保存新的构建次数和日期
    buildCountFile.writeText("$buildCount")
    lastBuildDateFile.writeText(today)
    
    // versionCode = yyMMdd + 2 位构建次数 (01-99)
    return datePrefix * 100 + buildCount
}

// 自动生成版本名称：1.x.x-YYYYMMDD-HHMM
fun generateVersionName(): String {
    val date = Date()
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HHmm", Locale.getDefault())
    return "1.${dateFormat.format(date)}.${timeFormat.format(date)}"
}

android {
    namespace = "com.syncrime.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.syncrime.android"
        minSdk = 24
        targetSdk = 36
        versionCode = generateVersionCode(projectDir)
        versionName = generateVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        buildConfigField("String", "API_BASE_URL", "\"https://syncrime-api.claw.carc.top\"")
        buildConfigField("String", "SYNC_SERVER_URL", "\"https://syncrime-api.claw.carc.top\"")
        buildConfigField("long", "SYNC_INTERVAL", "900000") // 15 分钟
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", "\"https://syncrime-api.claw.carc.top\"")
            buildConfigField("String", "SYNC_SERVER_URL", "\"https://syncrime-api.claw.carc.top\"")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // 自定义 APK 输出文件名
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = "SyncRime"
            val version = variant.versionName
            val buildType = variant.buildType.name
            output.outputFileName = "${appName}-v${version}-${buildType}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // 项目模块依赖 (暂时注释，等待模块配置修复)
    // implementation(project(":shared"))
    // implementation(project(":inputmethod"))
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Activity Compose
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Android Core Libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    
    // Android Test dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug dependencies
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Kotlin Compose Compiler
kotlin {
    jvmToolchain(17)
}

// 编译前清理旧的 APK 文件任务
tasks.register("cleanOldApks") {
    doLast {
        val debugApkDir = File("$buildDir/outputs/apk/debug")
        val releaseApkDir = File("$buildDir/outputs/apk/release")
        
        if (debugApkDir.exists()) {
            debugApkDir.listFiles { file -> file.extension == "apk" }?.forEach { 
                it.delete()
                println("🧹 已删除：${it.name}")
            }
        }
        if (releaseApkDir.exists()) {
            releaseApkDir.listFiles { file -> file.extension == "apk" }?.forEach { 
                it.delete()
                println("🧹 已删除：${it.name}")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("cleanOldApks")
}
