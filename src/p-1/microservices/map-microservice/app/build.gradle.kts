plugins {
    java
    application
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/") // Per il deserializer Avro Confluent
}

dependencies {
    implementation("io.vertx:vertx-core:4.4.0")
    implementation("io.vertx:vertx-web:4.4.0")
    implementation("io.vertx:vertx-web-client:4.4.0")
    implementation("io.vertx:vertx-config:4.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    // Metrics & Logging
    implementation("io.micrometer:micrometer-core:1.12.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")
    implementation("io.vertx:vertx-micrometer-metrics:4.4.0")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Kafka client + Avro + Schema Registry
    implementation("org.apache.kafka:kafka-clients:3.9.0")
    implementation("io.confluent:kafka-avro-serializer:7.5.0")
    implementation("org.apache.avro:avro:1.11.3")

    // Jackson (per fallback, opzionale)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Test Vert.x
    testImplementation("io.vertx:vertx-junit5:4.4.0")
}

application {
    mainClass = "Main"
}

tasks.jar {
    archiveFileName.set("app.jar")
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
    from(sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
