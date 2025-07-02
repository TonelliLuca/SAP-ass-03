plugins {
    java
    application
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")

}

dependencies {
    // Vertx
    implementation(platform("io.vertx:vertx-stack-depchain:4.4.0"))
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-web-client")
    implementation("io.vertx:vertx-mongo-client")
    implementation("io.vertx:vertx-config:4.4.0")
    // MongoDB
    implementation("org.mongodb:mongodb-driver-reactivestreams:4.11.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.vertx:vertx-junit5")
    testImplementation("org.mockito:mockito-core:5.3.1")

    // Micrometer e Prometheus
    implementation("io.micrometer:micrometer-core:1.12.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")
    implementation("io.vertx:vertx-micrometer-metrics:4.4.0")

    // Kafka Client
    implementation("org.apache.kafka:kafka-clients:3.9.0")

    // Avro core
    implementation("org.apache.avro:avro:1.11.3")
    // Confluent Avro Serializer/Deserializer per Kafka/Schema Registry
    implementation("io.confluent:kafka-avro-serializer:7.5.3")

    // Jackson per JSON fallback
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("Main")
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

avro {
    stringType.set("String")
}
