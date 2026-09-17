plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

group = "com.kakaanime"
version = "0.1.0"

dependencies {
    implementation(project(":provider-core"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ani.anilab.backend.BackendKt")
}
