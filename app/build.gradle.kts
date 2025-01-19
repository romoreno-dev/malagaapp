val VERSION: String = "0.0.1"
val STATE_VERSION: String = ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.plugin)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.secrets.plugin)
    alias(libs.plugins.google.service)
    alias(libs.plugins.crashlytics)
}


android {
    namespace = "com.romoreno.malagapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.romoreno.malagapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "${VERSION}${STATE_VERSION}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isDebuggable = false
            resValue("string", "malagapp_name", "Malagapp")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isDebuggable = true
            resValue("string", "malagapp_name", "Malagapp${STATE_VERSION}")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

secrets {
    // Para setear la clave de Google Maps en el proyecto
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {

    // Android y Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // DaggerHilt (Dependencia de DaggerHilt para Android y del compilador. Necesita el plugin de
    // kapt y el de dagger (desactivando su ejecucion inmediata)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Retrofit  (Retrofit, Gson y Logger HTTP)
    implementation(libs.retrofit2.retrofit)
    implementation(libs.retrofit2.gson)
    implementation(libs.okhttp.logging)

    // NavComponent (Con esto se añade la navegacion, el delegado de los viewModel para descargarse
    // el control de su ciclo de vida, corrutinas, etc)
    // Tambien plugin de safeargs para pasar datos entre vistas
    implementation(libs.navigation.ui)
    implementation(libs.navigation.fragment)

    // Room y DataStore Preferences (tambien podriamos haber usado las SharedPreferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Google Maps
    implementation(libs.google.maps)
    implementation(libs.google.location)
    implementation(libs.google.places)

    // Glide
    implementation(libs.bumptech.glide)

    // Splash Screen Api
    implementation(libs.androidx.core.splashscreen)

    // Swipe Refresh Layout
    implementation(libs.androidx.swiperefreshlayout)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.realtime)
    // (Acceso con Google para Android, finalmente no se usara)
//    implementation ("com.google.android.gms:play-services-auth:20.5.0")
    // (Validacion de cuenta mediante SMS, finalmente no se usara)
//    implementation ("com.google.android.gms:play-services-auth-api-phone")
    // (Libreria de logging, finalmente no se usara)
//    implementation("com.jakewharton.timber:timber:5.0.1")

    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.runner.junit)
    testImplementation(libs.io.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.android.core.testing)

    // Android Tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}