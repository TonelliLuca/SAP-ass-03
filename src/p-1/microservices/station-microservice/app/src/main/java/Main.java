import application.ports.DomainEventPublisher;
import application.ports.Service;
import application.service.StationService;
import domain.model.P2d;
import domain.model.Station;
import infrastructure.adapter.kafka.StationProducer;
import infrastructure.repository.InMemoryRepository;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        // 1. Create repository
        InMemoryRepository repo = new InMemoryRepository();

        // 2. Create Kafka producer (replace with your Kafka bootstrap servers)
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        DomainEventPublisher publisher = new StationProducer(bootstrapServers);

        // 3. Create service
        Service service = new StationService(repo, publisher);

        // 4. Register a station
        Station station = new Station("station-1", new P2d(10.0, 20.0), 4);
        repo.save(station).join();

        // 5. Initialize service (publishes StationRegisteredEvent for all stations)
        service.init();

        // 6. Start a simple HTTP server for healthcheck
        startHealthServer();

        System.out.println("Microservice started.");
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