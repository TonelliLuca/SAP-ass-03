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
            String bootstrapServers = config.getKakaConf();
            ProjectionRepositoryPort localProjections = new LocalProjectionRepository();
            RideEventsProducerPort producer = new RideEventsProducer(bootstrapServers, "http://schema-registry:8091", vertx);
            RestRideServiceAPI service = new RestRideServiceAPIImpl(
                new EventPublisherImpl(vertx),
                vertx,
                localProjections,
                producer
            );
            ProjectionUpdatesConsumer updatesConsumer = new ProjectionUpdatesConsumer(
                    bootstrapServers,
                    "http://schema-registry:8091",
                    localProjections,
                    service
                    );
            updatesConsumer.init();
            RideServiceVerticle rideServiceVerticle = new RideServiceVerticle(service, vertx);
            rideServiceVerticle.init();
            producer.init();
            System.out.println("Ride microservice started successfully");
        });
    }
}