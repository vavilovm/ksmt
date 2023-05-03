import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.ksmt.ksmt-base")
    id("com.adarshr.test-logger") version "3.2.0"
}

dependencies {
    implementation(project(mapOf("path" to ":ksmt-core")))
    testImplementation(project(mapOf("path" to ":ksmt-z3")))
    testImplementation(project(mapOf("path" to ":ksmt-bitwuzla")))
    testImplementation(project(mapOf("path" to ":ksmt-yices")))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=all")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["kotlinSourcesJar"])
        }
    }
}

repositories {
    mavenCentral()
}


testlogger {
    showStandardStreams = true
}
