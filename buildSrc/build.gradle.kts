plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")

    implementation(gradleApi())
}

repositories {
    mavenCentral()
    google()
}
