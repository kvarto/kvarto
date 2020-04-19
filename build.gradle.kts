import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val vertxVersion = "3.8.5"
val coroutinesVersion = "1.3.3"

plugins {
    val kotlinVersion = "1.3.72"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    `maven-publish`
}

group = "io.kvarto"
version = "0.0.1"

java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven("https://oss.jfrog.org/artifactory/oss-snapshot-local")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-web-client:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-circuit-breaker:$vertxVersion")
    implementation("io.micrometer:micrometer-core:1.3.2")
    implementation("io.opentracing:opentracing-api:0.33.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.10")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.7") { exclude("org.jetbrains.kotlin") }
    implementation("io.github.classgraph:classgraph:4.8.58")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.5.2")
    testImplementation("org.assertj:assertj-core:3.14.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kvarto/kvarto")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register("gpr", MavenPublication::class) {
            from(components["java"])
        }
    }
}
