plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("26.2.build.+")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}