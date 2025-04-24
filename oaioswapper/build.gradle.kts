plugins {
    id("java")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.runelite.net") }
}

dependencies {
    implementation(project(":kotoriutils")) // 👈️ this links ReflectionLibrary
    compileOnly("net.runelite:client:1.10.11")
    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")
}
