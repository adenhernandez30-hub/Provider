import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "com.kakaanime"
version = "0.1.0"

dependencies {
    implementation(project(":provider-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("org.json:json:20240303")
    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile>().configureEach {
    exclude("**/android/**")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed", "standard_out", "standard_error")
    }
}
