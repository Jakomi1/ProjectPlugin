plugins {
    id("java-library")
    id("maven-publish")
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}