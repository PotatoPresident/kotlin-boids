plugins {
    kotlin("js") version "1.5.31"
}

group = "io.github.potatopresident"
version = "1.0"

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }

    val doodleVersion = "0.6.0"

    dependencies {
        implementation ("io.nacular.doodle:core:$doodleVersion"   )
        implementation ("io.nacular.doodle:browser:$doodleVersion")
        implementation ("io.nacular.doodle:controls:$doodleVersion" )
        // implementation ("io.nacular.doodle:animation:$doodleVersion")
        // implementation ("io.nacular.doodle:themes:$doodleVersion"   )

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.5.2")
    }
}

rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
    versions.webpackCli.version = "4.9.1"
}