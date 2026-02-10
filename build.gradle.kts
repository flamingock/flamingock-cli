plugins {
    id("flamingock.java-library")
    id("flamingock.license")
    id("org.graalvm.buildtools.native") version "0.10.6"
    `maven-publish`
}

description = "Flamingock CLI for executing changes in applications"

val jacksonVersion = "2.16.0"
val picocliVersion = "4.7.5"
val flamingockVersion = "1.0.1"

repositories {
    mavenLocal()  // For local development with unpublished versions
    mavenCentral()
}

dependencies {
    implementation("io.flamingock:flamingock-core-commons:$flamingockVersion")

    // CLI Framework
    implementation("info.picocli:picocli:$picocliVersion")
    annotationProcessor("info.picocli:picocli-codegen:$picocliVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

// Create UberJar with all dependencies
val uberJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Create a self-contained JAR with all dependencies"

    archiveBaseName.set("flamingock-cli")
    archiveClassifier.set("uber")
    archiveVersion.set(project.version.toString())
    isZip64 = true

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "io.flamingock.cli.executor.FlamingockExecutorCli"
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
    }

    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}

// Test configuration
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

tasks.named("assemble").configure {
    dependsOn(uberJar)
}

// GraalVM Native Image configuration
graalvmNative {
    binaries {
        named("main") {
            imageName.set("flamingock")
            mainClass.set("io.flamingock.cli.executor.FlamingockExecutorCli")
            sharedLibrary.set(false)
            buildArgs.addAll(
                "--no-fallback",
                "--install-exit-handlers"
            )
        }
    }
}

// Generate version properties file for native image (no MANIFEST.MF available)
val generateVersionProperties by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources/version")
    outputs.dir(outputDir)
    doLast {
        val propsFile = outputDir.get().file("flamingock-cli-executor.properties").asFile
        propsFile.parentFile.mkdirs()
        propsFile.writeText("version=${project.version}\n")
    }
}

sourceSets.main {
    resources.srcDir(generateVersionProperties.map { it.outputs.files.singleFile })
}

tasks.named("processResources").configure {
    dependsOn(generateVersionProperties)
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifact(uberJar.get()) {
                classifier = "uber"
            }

            pom {
                name.set("Flamingock CLI")
                description.set("Flamingock CLI for executing changes in applications")
                url.set("https://www.flamingock.io")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("flamingock")
                        name.set("Flamingock Team")
                        email.set("info@flamingock.io")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/flamingock/flamingock-cli.git")
                    developerConnection.set("scm:git:ssh://github.com/flamingock/flamingock-cli.git")
                    url.set("https://github.com/flamingock/flamingock-cli")
                }
            }
        }
    }
}
