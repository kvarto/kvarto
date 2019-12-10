import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "1.3.61"
val vertxVersion = "3.8.4"
val coroutinesVersion = "1.3.2"

plugins {
    kotlin("jvm") version "1.3.60"
}

group = "io.kvarto"
version = "0.0.1"


java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven("https://oss.jfrog.org/artifactory/oss-snapshot-local")
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-web-client:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-circuit-breaker:$vertxVersion")
    implementation("org.apache.httpcomponents:httpclient:4.5.10")
    implementation("io.opentelemetry:opentelemetry-api:0.2.0-SNAPSHOT")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.7") { exclude("org.jetbrains.kotlin") }
    implementation("io.github.classgraph:classgraph:4.8.58")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testImplementation("org.assertj:assertj-core:3.14.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
