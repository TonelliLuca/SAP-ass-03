import application.ports.DomainEventPublisher;
import application.ports.Service;
import application.ports.StationRepository;
import application.service.StationService;
import domain.model.P2d;
import domain.model.Station;
import infrastructure.adapter.kafka.StationProducer;
import infrastructure.config.ServiceConfiguration;
import infrastructure.repository.MongoRepository;

import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoClient;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        // 1. Load configuration
        ServiceConfiguration config = ServiceConfiguration.getInstance();

        // 2. Create MongoDB client using config
        MongoClient mongoClient = MongoClients.create(config.getMongoConnection());
        StationRepository repo = new MongoRepository(mongoClient);

        // 3. Create Kafka producer using config
        DomainEventPublisher publisher = new StationProducer(config.getKafkaBootstrapServers());

        // 4. Create service
        Service service = new StationService(repo, publisher);


        // 5. Initialize service
        service.init();

        // 6. Start health check server
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