plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.aboutLibraries)
}

android {
    namespace = "com.lonx.lyrico"
    ndkVersion = "29.0.14206865"
    compileSdk {
        version = release(37)
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
    defaultConfig {
        applicationId = "com.lonx.lyrico"
        minSdk = 28
        targetSdk = 36
        versionCode = 18
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    // ============================================================================
    // 签名配置：使用固定 keystore（通过环境变量传入，CI 构建时设置）
    // 这样每次 CI 构建产出的 APK 签名一致，覆盖安装不会丢数据。
    // 本地开发无环境变量时，回退到默认 debug 签名。
    // ============================================================================
    signingConfigs {
        create("ci") {
            val storeFilePath = System.getenv("LYRICO_KEYSTORE_PATH")
            val storePass = System.getenv("LYRICO_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("LYRICO_KEY_ALIAS")
            val keyPass = System.getenv("LYRICO_KEY_PASSWORD")
            if (storeFilePath != null && storePass != null && keyAlias != null && keyPass != null) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                this.keyAlias = keyAlias
                keyPassword = keyPass
                // PKCS12 keystore 的 storePassword 和 keyPassword 必须相同
            }
        }
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // CI 构建时用固定签名，否则用默认 release 签名
            System.getenv("LYRICO_KEYSTORE_PATH")?.let {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // CI 构建时用固定签名，否则用默认 debug 签名
            System.getenv("LYRICO_KEYSTORE_PATH")?.let {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
dependencies {
    // Project Modules
    implementation(project(":lyrico-audiotag"))

    // network
    implementation(libs.okhttp)
    // JSON 解析
    implementation(libs.kotlinx.serialization.json)
    // Compose & UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.destinations.core)
    implementation(libs.miuix.preference.android)
    implementation(libs.miuix.ui.android)
    implementation(libs.miuix.icons.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.reorderable)
    implementation(libs.compose.markdown)
    implementation(libs.lazycolumnscrollbar)
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    // Dependency Injection
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Utilities
    implementation(libs.devicecompat)
    implementation(libs.tinypinyin)
    implementation(libs.opencc4j)
    implementation(libs.aboutlibraries.compose.core)
    // KSP
    ksp(libs.compose.destinations.ksp)
    ksp(libs.androidx.room.compiler)

    // Testing
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
