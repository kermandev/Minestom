import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(rootProject)

    runtimeOnly(libs.bundles.logback)
}

tasks {
    compileJava {
        options.compilerArgs.add("--enable-preview")
        //options.allCompilerArgs.add("--enable-preview")
    }

    application {
        mainClass.set("net.minestom.demo.Main")
    }

    withType<ShadowJar> {
        archiveFileName.set("minestom-demo.jar")
    }
}