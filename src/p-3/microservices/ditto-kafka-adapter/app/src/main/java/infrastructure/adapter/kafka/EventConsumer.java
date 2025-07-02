package infrastructure.adapter.kafka;

import application.port.DittoTranslatorServicePort;
import domain.model.*;
import org.apache.avro.generic.GenericRecord;

public class EventConsumer {
    private final AvroKafkaConsumer abikeConsumer;
    private final AvroKafkaConsumer stationConsumer;
    private final GenericKafkaConsumer<String> dittoConsumer;
    private final DittoTranslatorServicePort service;

    public EventConsumer(String bootstrapServer, String schemaRegistryUrl, DittoTranslatorServicePort service) {
        this.abikeConsumer = new AvroKafkaConsumer(
                bootstrapServer,
                schemaRegistryUrl,
                "ditto-translator-abike",
                "abike-events"
        );
        this.stationConsumer = new AvroKafkaConsumer(
                bootstrapServer,
                schemaRegistryUrl,
                "ditto-translator-station",
                "station-events"
        );
        this.dittoConsumer = new GenericKafkaConsumer<>(
                bootstrapServer,
                "ditto-inbox-consumer",
                "ditto-commands",
                String.class
        );

        this.service = service;
    }

    public void init() {
        abikeConsumer.start(this::processAbikeEvent);
        stationConsumer.start(this::processStationEvent);
        dittoConsumer.start(this::handleDittoEvent);
    }

    private void processAbikeEvent(String key, GenericRecord envelopeRecord) {
        GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
        if (eventRecord == null) return;
        // Avro diretto su ABikeUpdate
        String schemaName = eventRecord.getSchema().getName();
        if (!"ABikeUpdate".equals(schemaName)) {
            return;
        }
        try {
            String id = eventRecord.get("id").toString();
            String timestamp = eventRecord.get("timestamp").toString();
            GenericRecord abikeRecord = (GenericRecord) eventRecord.get("abike");

            String abikeId = abikeRecord.get("id").toString();
            double x = (Double) abikeRecord.get("x");
            double y = (Double) abikeRecord.get("y");
            String state = abikeRecord.get("state").toString();
            int battery = (Integer) abikeRecord.get("batteryLevel");

            ABike abike = new ABike(abikeId, new Location(x, y), battery, state);
            ABikeUpdateEvent evt = new ABikeUpdateEvent(id, abike, "ABikeUpdate", timestamp);

            service.handleEvent(evt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processStationEvent(String key, GenericRecord envelopeRecord) {
        try {
            // Envelope: prendi "event"
            GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
            if (eventRecord == null) return;

            String schemaName = eventRecord.getSchema().getName();
            if ("StationUpdateEventAvro".equals(schemaName) || "StationRegisteredEventAvro".equals(schemaName)) {
                String id = eventRecord.get("id").toString();
                String timestamp = eventRecord.get("timestamp").toString();
                GenericRecord stationRecord = (GenericRecord) eventRecord.get("station");

                String stationId = stationRecord.get("id").toString();
                GenericRecord location = (GenericRecord) stationRecord.get("location");
                double x = (Double) location.get("x");
                double y = (Double) location.get("y");
                int capacity = (Integer) stationRecord.get("capacity");
                // dockedBikes = ArrayList<String>
                java.util.List<String> dockedBikes = new java.util.ArrayList<>();
                for (Object s : (java.util.Collection<?>) stationRecord.get("dockedBikes")) {
                    dockedBikes.add(s.toString());
                }
                Station station = new Station(stationId, new Location(x, y), capacity, dockedBikes, capacity - dockedBikes.size());
                StationUpdateEvent evt = new StationUpdateEvent(id, station, timestamp);
                service.handleEvent(evt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDittoEvent(String key, String message) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(message);
            String topic = root.path("topic").asText();
            String[] topicParts = topic.split("/");
            String thingId = topicParts.length > 1 ? topicParts[1] : null;
            String correlationId = root.path("headers").path("correlation-id").asText();
            String replyTarget = root.path("headers").has("ditto-reply-target")
                    ? root.path("headers").path("ditto-reply-target").asText()
                    : "0";
            service.sendDittoResponse(thingId, correlationId, "ok", null, replyTarget);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        abikeConsumer.stop();
        stationConsumer.stop();
        dittoConsumer.stop();
    }
}
