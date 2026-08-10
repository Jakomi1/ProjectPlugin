plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.5.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = findProperty("group") ?: "de.jakomi1"
version = findProperty("version") ?: "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("26.2.build.+")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveBaseName.set("ProjectPlugin")
        archiveVersion.set("")
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    clean {
        delete(layout.buildDirectory.dir("libs"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar)
        }
    }
}