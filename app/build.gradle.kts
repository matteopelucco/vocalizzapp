import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

// Credenziali di firma release: in keystore.properties (non versionato), la chiave sta fuori dal repo.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Versione: major.minor.patch + numero di build, da version.properties (vedi il file).
// Una build che firma l'APK usa i valori correnti e poi aumenta patch e code per la firma successiva.
val versionFile = rootProject.file("version.properties")
val versionProps = Properties().apply { versionFile.inputStream().use { load(it) } }
val vMajor = versionProps.getProperty("major").toInt()
val vMinor = versionProps.getProperty("minor").toInt()
val vPatch = versionProps.getProperty("patch").toInt()
val vCode = versionProps.getProperty("code").toInt()
val isSigningBuild = gradle.startParameter.taskNames.any {
    Regex("(assemble|bundle|install|package)Release$").containsMatchIn(it)
}
if (isSigningBuild) {
    versionFile.writeText(
        "# Versione del PROSSIMO APK firmato (assembleRelease / bundleRelease / installRelease).\n" +
            "# A ogni firma \"patch\" e \"code\" salgono di 1 in automatico: dopo la firma committare questo file.\n" +
            "# \"major\" e \"minor\" si cambiano a mano, solo su richiesta dell'utente.\n" +
            "major=$vMajor\nminor=$vMinor\npatch=${vPatch + 1}\ncode=${vCode + 1}\n"
    )
}
// Data e ora della build, mostrate all'utente accanto alla versione.
val buildStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.matteopelucco.vocalizzapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.matteopelucco.vocalizzapp"
        minSdk = 26
        targetSdk = 36
        versionCode = vCode
        versionName = "$vMajor.$vMinor.$vPatch"
        buildConfigField("String", "BUILD_STAMP", "\"$buildStamp\"")
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (!keystoreProps.isEmpty) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Distingue a colpo d'occhio una build di sviluppo da una firmata con lo stesso numero.
            versionNameSuffix = "-dev"
        }
        release {
            // Niente R8: le classi JNI di sherpa-onnx vanno trovate per nome dal codice nativo.
            isMinifyEnabled = false
            if (!keystoreProps.isEmpty) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
}
