plugins {
    id("java")
    id("jacoco")
    id("io.freefair.lombok") version "4.1.6"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenLocal()
    mavenCentral()
}

version = "1.2.1"

