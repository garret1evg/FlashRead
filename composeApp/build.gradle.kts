import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// kxml2 2.3.0 embeds org.xmlpull.v1, which Android already provides.
// Shipping those classes makes R8 fail: framework XmlResourceParser would
// implement a program class. Host JVM tests still need the full jar.
val kxml2Classpath = configurations.create("kxml2Classpath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    kxml2Classpath(libs.kxml2)
}

abstract class StripXmlPullJar : DefaultTask() {
    @get:InputFiles
    abstract val inputJars: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun strip() {
        val input = inputJars.singleFile
        val output = outputJar.get().asFile
        output.parentFile.mkdirs()
        ZipFile(input).use { zip ->
            ZipOutputStream(output.outputStream().buffered()).use { zos ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith("org/xmlpull/")) continue
                    zos.putNextEntry(ZipEntry(entry.name))
                    if (!entry.isDirectory) {
                        zip.getInputStream(entry).use { it.copyTo(zos) }
                    }
                    zos.closeEntry()
                }
            }
        }
    }
}

val stripKxml2XmlPull by tasks.registering(StripXmlPullJar::class) {
    inputJars.from(kxml2Classpath)
    outputJar.set(layout.buildDirectory.file("stripped-libs/kxml2-noxmlpull.jar"))
}

kotlin {
    android {
        namespace = "com.evgeniich.flashread.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        // Required so composeResources (app_logo.png) are packaged into the Android APK.
        // See https://youtrack.jetbrains.com/issue/CMP-9547
        androidResources.enable = true
        withHostTest {}
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.uiToolingPreview)
            implementation(files(stripKxml2XmlPull.flatMap { it.outputJar }))
            implementation(libs.timber)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.user.messaging.platform)
            implementation(libs.play.services.ads)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kxml2)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.jetbrains.navigation3.runtime)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.evgeniich.flashread.resources"
}

