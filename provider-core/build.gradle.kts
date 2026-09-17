plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "com.kakaanime.provider"
version = "0.1.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.3")
}

kotlin {
    jvmToolchain(17)
}
