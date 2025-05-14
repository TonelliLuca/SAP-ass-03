import application.RestRideServiceAPIImpl;
import application.ports.*;
import infrastructure.adapter.kafka.ProjectionUpdatesConsumer;
import infrastructure.adapter.kafka.RideEventsProducer;
import infrastructure.repository.LocalProjectionRepository;
import infrastructure.utils.EventPublisherImpl;
import infrastructure.adapter.web.RideServiceVerticle;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.Vertx;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            System.out.println("Configuration loaded: " + conf.encodePrettily());

            // Get Kafka configuration
            String bootstrapServers = config.getKakaConf("kafka.bootstrapServers", "kafka:29092");

            // Create shared projection repository
            LocalProjectionRepository localProjections = new LocalProjectionRepository();

            // Create Kafka event producer
            RideEventsProducerPort producer = new RideEventsProducer(bootstrapServers);

            // Create and initialize projection updates consumer
            ProjectionUpdatesConsumer updatesConsumer = new ProjectionUpdatesConsumer(
                bootstrapServers,
                localProjections
            );
            updatesConsumer.init();

            // Create REST API service implementation
            RestRideServiceAPI service = new RestRideServiceAPIImpl(
                new EventPublisherImpl(vertx),
                vertx,
                localProjections,
                producer
            );

            // Create and initialize web service verticle
            RideServiceVerticle rideServiceVerticle = new RideServiceVerticle(service, vertx);
            rideServiceVerticle.init();

            System.out.println("Ride microservice started successfully");
        });
    }
}