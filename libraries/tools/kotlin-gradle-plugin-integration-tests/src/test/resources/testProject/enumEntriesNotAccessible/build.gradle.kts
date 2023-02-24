import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = "com.example"
version = "1.0"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
