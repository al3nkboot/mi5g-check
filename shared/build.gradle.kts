import com.kgurgul.AndroidVersions
import com.kgurgul.KoverConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kover)
    alias(libs.plugins.licenses)
    alias(libs.plugins.serialization)
    id("kotlin-parcelize")
}

version = "1.0"

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.kgurgul.cpuinfo.shared"
        compileSdk = AndroidVersions.COMPILE_SDK
        minSdk = AndroidVersions.MIN_SDK

        androidResources {
            enable = true
        }

        withHostTestBuilder {}.configure { isIncludeAndroidResources = true }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.js.ExperimentalWasmJsInterop")
            }
        }

        commonMain {
            dependencies {
                implementation(libs.androidx.datastore.preferences.core)
                api(libs.coil)
                implementation(libs.jetbrains.compose.adaptive)
                implementation(libs.jetbrains.compose.adaptive.navigationSuit)
                api(libs.jetbrains.compose.componentsResources)
                api(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.lifecycle.viewmodel)
                implementation(libs.jetbrains.compose.lifecycle.runtime)
                implementation(libs.jetbrains.compose.material.iconsExtended)
                api(libs.jetbrains.compose.material3)
                api(libs.jetbrains.compose.navigation)
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.jetbrains.compose.uiToolingPreview)
                implementation(libs.kermit.kermit)
                api(libs.koin.compose.viewodel)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.immutable)
                api(libs.kotlinx.serialization)
            }
        }

        val mobileMain = create("mobileMain") { dependsOn(commonMain.get()) }

        androidMain {
            dependsOn(mobileMain)
            dependencies {
                implementation(projects.nativeAndroid)
                api(libs.androidx.activity.compose)
                implementation(libs.androidx.core)
                api(libs.androidx.core.splashscreen)
                implementation(libs.androidx.datastore.preferences)
                api(libs.androidx.tv)
                api(libs.koin.android)
                implementation(libs.relinker)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))

                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.arch.core.testing)
                implementation(libs.koin.test)
                implementation(libs.kotlinx.coroutines.test)
                // Robolectric screenshot tests (Arabic RTL)
                implementation("org.robolectric:robolectric:4.16")
                implementation(libs.compose.ui.test)
                implementation(libs.compose.ui.testManifest)
            }
        }
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.get().compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.kgurgul.cpuinfo.shared"
    generateResClass = always
}

composeCompiler {
    // Opt-in only: the Compose compiler metrics/reports writer crashes on Windows
    // ("Invalid file path") under Kotlin 2.4.0. Enable with -PenableComposeCompilerReports=true.
    if (providers.gradleProperty("enableComposeCompilerReports").orNull == "true") {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

dependencies {
    "androidRuntimeClasspath"(libs.jetbrains.compose.uiTooling)
}

kover {
    reports {
        total {
            filters {
                includes {
                    packages(KoverConfig.includedPackages)
                }
                excludes {
                    packages(KoverConfig.excludedPackages)
                    classes(KoverConfig.excludedClasses)
                    annotatedBy(KoverConfig.excludedAnnotations)
                }
            }

            html {
                htmlDir = layout.buildDirectory.dir("coverage-report/html")
            }

            xml {
                xmlFile = layout.buildDirectory.file("coverage-report/result.xml")
            }
        }
    }
}
