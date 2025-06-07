
import application.DittoTranslatorService;
import application.port.DittoProducerPort;
import application.port.DittoTranslatorServicePort;
import com.sun.net.httpserver.HttpServer;
import infrastructure.adapter.kafka.DittoProducerKafkaAdapter;
import infrastructure.adapter.kafka.EventConsumer;
import infrastructure.config.ServiceConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        // Load configuration
        ServiceConfiguration config = ServiceConfiguration.getInstance();

        // Set up Ditto producer
        String dittoTopic = "ditto-messages"; // or get from config/env
        DittoProducerPort producer = new DittoProducerKafkaAdapter(config.getKafkaBootstrapServers(), dittoTopic);

        // Set up translator service
        DittoTranslatorServicePort service = new DittoTranslatorService(producer);

        // Start event consumers
        EventConsumer consumer = new EventConsumer(config.getKafkaBootstrapServers(), service);

        startHealthServer();
        // Keep main thread alive
        System.out.println("Ditto Kafka Adapter is running...");
    }
    private static void startHealthServer() {
        try {
            int port = Integer.parseInt(System.getenv().getOrDefault("SERVICE_PORT", "8083"));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/health", exchange -> {
                String response = "{\"status\":\"UP\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            });
            server.setExecutor(null);
            server.start();
            System.out.println("Health check endpoint available at http://localhost:" + port + "/health");
        } catch (IOException e) {
            System.err.println("Failed to start health check server: " + e.getMessage());
        }
    }
}