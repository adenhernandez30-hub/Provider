plugins {
    kotlin("jvm") version "2.1.20"
    application
}

group = "com.kakaanime"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":provider-core"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ani.anilab.backend.BackendKt")
}
