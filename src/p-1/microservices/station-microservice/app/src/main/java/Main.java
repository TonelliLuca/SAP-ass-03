import application.ports.DomainEventPublisher;
import application.ports.Service;
import application.ports.StationRepository;
import application.service.StationService;
import domain.model.P2d;
import domain.model.Station;
import infrastructure.adapter.kafka.StationConsumer;
import infrastructure.adapter.kafka.StationProducer;
import infrastructure.adapter.web.RESTStationAdapter;
import infrastructure.adapter.web.StationVerticle;
import infrastructure.config.ServiceConfiguration;
import infrastructure.repository.MongoRepository;


import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;

import com.sun.net.httpserver.HttpServer;
import io.vertx.core.Vertx;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            // 2. Create MongoDB client using config (using standard driver now)
            MongoClient mongoClient = MongoClients.create(config.getMongoConfig().getString("connection_string"));
            StationRepository repo = new MongoRepository(mongoClient, config.getMongoConfig().getString("db_name"), config.getMongoConfig().getString("collection_name"));

            // 3. Create Kafka producer using config
            DomainEventPublisher publisher = new StationProducer(config.getKakaConf());

            // 4. Create service
            Service service = new StationService(repo, publisher);
            RESTStationAdapter restAdapter = new RESTStationAdapter(service, vertx);
            StationVerticle verticle = new StationVerticle(restAdapter, vertx);
            StationConsumer consumer = new StationConsumer(config.getKakaConf(), service);
            // 5. Initialize service
            verticle.init();
            service.init();
            consumer.init();

            System.out.println("Microservice started.");
        });
    }
}

