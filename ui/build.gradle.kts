plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.compose)
}
repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

compose.resources {
    packageOfResClass = "com.xdmrwu.recompose.spy.plugin.generated"
}

kotlin {
    jvm()

    sourceSets {

        commonMain.dependencies {
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}