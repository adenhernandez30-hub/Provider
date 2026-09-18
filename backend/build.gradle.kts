plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

group = "com.kakaanime"
version = "0.1.0"

dependencies {
    implementation(project(":provider"))
    implementation(project(":provider-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("ani.anilab.backend.BackendKt")
}
