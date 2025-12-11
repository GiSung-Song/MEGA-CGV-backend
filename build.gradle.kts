import org.asciidoctor.gradle.jvm.AsciidoctorTask

plugins {
    java
    id("org.springframework.boot") version "3.4.8"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "3.3.2"
}

val querydslVersion = "5.0.0"
val generatedDir = "src/main/generated"
val snippetsDir by extra { file("build/generated-snippets") }

group = "com.cgv"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // core
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("me.paulschwarz:spring-dotenv:3.0.0")

    // ES
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // querydsl
    implementation("com.querydsl:querydsl-jpa:${querydslVersion}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${querydslVersion}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // web client
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // db/dev
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.mysql:mysql-connector-j")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:mysql:1.17.6")
    testImplementation("org.testcontainers:elasticsearch")

    // restdocs
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.restdocs:spring-restdocs-asciidoctor")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(file(generatedDir))
    options.compilerArgs.add("-parameters")
}

sourceSets {
    named("main") {
        java.srcDir(generatedDir)
    }
}

tasks.named<Delete>("clean") {
    delete(file(generatedDir))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("generateQueryDSL") {
    dependsOn("compileJava")
    group = "build"
    description = "Generate only QueryDSL Q-types without running tests"
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    outputs.dir(snippetsDir)
}

tasks.withType<AsciidoctorTask> {
    inputs.dir(snippetsDir)
    dependsOn(tasks.named("integrationTest"))
    baseDirFollowsSourceDir()
}

tasks.register<Copy>("copyIndexDoc") {
    dependsOn(tasks.named("asciidoctor"))
    from(layout.buildDirectory.file("docs/asciidoc/index.html"))
    into(layout.projectDirectory.dir("src/main/resources/static/docs"))
}