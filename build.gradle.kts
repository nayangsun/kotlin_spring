plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.kotlin.jpa)
	alias(libs.plugins.detekt)
}

group = "com.kotlinspring"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
	testImplementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

	implementation(libs.springdoc.openapi)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.spring.boot.starter.validation)
	runtimeOnly(libs.flyway.database.postgresql)
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.kotlin.reflect)
	implementation(libs.jackson.module.kotlin)
	runtimeOnly(libs.postgresql)

	// Development helpers
	developmentOnly(libs.spring.boot.starter.test)

	// Test-only dependencies
	testImplementation(libs.embedded.postgres)
	testImplementation(libs.embedded.postgres.binaries.darwin.arm64)
	testImplementation(libs.embedded.postgres.binaries.darwin.amd64)
	testImplementation(libs.embedded.postgres.binaries.linux.amd64)

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions.core)
	testImplementation(libs.kotest.extensions.spring)
	testImplementation(libs.mockk)

	testRuntimeOnly(libs.h2)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("config/detekt/detekt.yml"))
}
