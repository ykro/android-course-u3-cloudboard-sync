import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ktlint)
}

// Modelo de Gemini para el power-up opcional. Se cambia con -Pcloudboard.geminiModel=<modelo>.
val geminiModel = providers.gradleProperty("cloudboard.geminiModel").getOrElse("gemini-3.8-flash")

android {
    namespace = "com.awa2027.cloudboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.awa2027.cloudboard"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GEMINI_MODEL", "\"$geminiModel\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Reloj virtual de kotlinx-coroutines-test (advanceUntilIdle, currentTime) en los tests.
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        // Calidad: con -PwarningsAsErrors=true (como en el CI) cualquier advertencia de Kotlin falla la compilación.
        allWarningsAsErrors.set(providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false))
    }
}

// Sin google-services.json la app compila y arranca en modo demo (fakes en memoria).
googleServices {
    missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}

// Los tests corren en la JVM: sin emulador, sin red y sin credenciales.
tasks.withType<Test>().configureEach {
    jvmArgs("-Xshare:off")
    // Paso 6 del taller: WorkshopTest lee las reglas; si cambian, los tests se vuelven a ejecutar.
    val rulesFile = rootProject.file("firebase/firestore.rules")
    inputs.file(rulesFile).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("cloudboard.rulesFile", rulesFile.absolutePath)
}

ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ai)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
}
