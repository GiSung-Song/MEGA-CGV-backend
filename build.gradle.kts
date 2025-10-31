plugins {
    java
    id("org.springframework.boot") version "3.4.8"
    id("io.spring.dependency-management") version "1.1.7"
}

val querydslVersion = "5.0.0"
val generatedDir = "src/main/generated"

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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    implementation("org.springframework.boot:spring-boot-starter-aop")

    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    implementation("com.querydsl:querydsl-jpa:${querydslVersion}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${querydslVersion}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:mysql:1.17.6")

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