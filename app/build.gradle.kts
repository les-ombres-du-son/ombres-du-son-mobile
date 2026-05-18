import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "fr.upjv.lesombresduson"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.upjv.lesombresduson"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Injection de vos 4 clés spécifiques :
        resValue("string", "app_name", localProperties.getProperty("APP_NAME", "Les Ombres du Son"))
        resValue("string", "Web_client_id", localProperties.getProperty("WEB_CLIENT_ID", ""))
        resValue("string", "database_url", localProperties.getProperty("DATABASE_URL", ""))
        resValue("string", "url_site_web", localProperties.getProperty("URL_SITE_WEB", ""))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    // App
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.play.services.auth)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.uiautomator)
    implementation(libs.lifecycle.common.jvm)
    implementation(libs.core.ktx)
    implementation(libs.firebase.database)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava)
    implementation(libs.gson)

    // Unit tests (local JVM)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)

    // Instrumented tests (device/emulator)
    androidTestImplementation(libs.junit.v115)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core.v351)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.mockk.android)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}