plugins {
    java
    application
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"   // AVRO PLUGIN
}

java {
    // Use Java 21.
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")   // Confluent per Avro Serializer
}

dependencies {
    // Vert.x
    implementation(platform("io.vertx:vertx-stack-depchain:4.4.0"))
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-web-client")
    implementation("io.vertx:vertx-mongo-client")
    implementation("io.vertx:vertx-config:4.4.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // MongoDB
    implementation("org.mongodb:mongodb-driver-reactivestreams:4.11.1")

    // Micrometer/Prometheus
    implementation("io.micrometer:micrometer-core:1.12.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")
    implementation("io.vertx:vertx-micrometer-metrics:4.4.0")

    // Kafka + Avro + Schema Registry
    implementation("org.apache.kafka:kafka-clients:3.9.0")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.0")

    // Jackson per fallback JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.vertx:vertx-junit5")
    testImplementation("org.mockito:mockito-core:5.3.1")
}

avro {
    stringType.set("String") // Buona pratica per Java
    fieldVisibility.set("PUBLIC") // Default, meglio NON cambiare
    // src/main/avro è il default path degli .avsc
}

application {
    mainClass.set("Main")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveFileName.set("app.jar")
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
