import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.google.protobuf.gradle.*

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "1.9.22"
    id("com.google.protobuf") version "0.9.4"
}

android {
    namespace = "app.mitra.matel"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.mitra.matel"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 11
        versionName = "0.9.811"
        
        // Add build number that increments with each build
        val buildNumberFile = rootProject.file("build_number.txt")
        val buildNumber = if (buildNumberFile.exists()) {
            val currentBuildNumber = buildNumberFile.readText().trim().toInt()
            val newBuildNumber = currentBuildNumber + 1
            buildNumberFile.writeText(newBuildNumber.toString())
            newBuildNumber
        } else {
            // Initial build number if file doesn't exist
            val initialBuildNumber = 1
            buildNumberFile.writeText(initialBuildNumber.toString())
            initialBuildNumber
        }
        buildConfigField("int", "BUILD_NUMBER", "$buildNumber")
    }

    signingConfigs {
        create("release") {
            val keystorePasswordEnv = System.getenv("KEYSTORE_PASSWORD")
            val keyPasswordEnv = System.getenv("KEY_PASSWORD")
            
            if (keystorePasswordEnv != null && keyPasswordEnv != null) {
                storeFile = rootProject.file("my-release-key.jks")
                storePassword = keystorePasswordEnv
                keyAlias = System.getenv("KEY_ALIAS") ?: "mitramatel"
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "IS_PRODUCTION", "false")
        }
        
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "IS_PRODUCTION", "true")
            signingConfig = signingConfigs.getByName("release")
            
            // Enable debug symbols for native code crash analysis
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            // Set the Kotlin JVM target via the new DSL
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    bundle {
        // Include native debug symbols in the bundle
        abi {
            enableSplit = true
        }
    }

    sourceSets {
        getByName("main") {
            proto {
                srcDir("src/main/proto")
            }
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.4"
    }
    plugins {
        create("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.58.0"
        }
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.58.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("java") {
                    option("lite")
                }
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
            task.builtins {
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    // AndroidX Compose BOM + libraries
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    // Compose Preview APIs (annotation and tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.36.0")

    // Coil for SVG support
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Ktor Client for HTTP requests
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // gRPC and Protobuf dependencies
    implementation("io.grpc:grpc-okhttp:1.58.0")
    implementation("io.grpc:grpc-protobuf-lite:1.58.0")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("com.google.protobuf:protobuf-kotlin-lite:3.24.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
}