import application.port.ABikeRepository;
import application.port.EventPublisher;
import application.port.SimulationRepository;
import application.port.StationProjectionRepository;
import application.service.ABikeServiceImpl;
import infrastructure.adapter.kafka.ABikeEventPublisher;
import infrastructure.adapter.kafka.ABikeProjectionUpdatesConsumer;
import infrastructure.adapter.web.ABikeVerticle;
import infrastructure.adapter.web.RESTABikeAdapter;
import infrastructure.configuration.ServiceConfiguration;
import infrastructure.repository.mongo.ABikeMongoRepository;
import infrastructure.repository.inMemory.SimulationInMemoryRepository;
import infrastructure.repository.inMemory.StationProjectionInMemoryRepository;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            System.out.println("Configuration loaded: " + conf.encodePrettily());
            MongoClient mongoClient = MongoClient.create(vertx, config.getMongoConfig());
            ABikeRepository abikeRepository = new ABikeMongoRepository(mongoClient);
            StationProjectionRepository stationRepository = new StationProjectionInMemoryRepository();
            SimulationRepository simulationRepository = new SimulationInMemoryRepository();
            String bootstrapServers = config.getKakaConf();
            EventPublisher eventPublisher = new ABikeEventPublisher(bootstrapServers);

            ABikeServiceImpl abikeService = new ABikeServiceImpl(
                abikeRepository,
                stationRepository,
                simulationRepository,
                eventPublisher,
                vertx
            );
            RESTABikeAdapter restABikeAdapter = new RESTABikeAdapter(abikeService);
            ABikeVerticle abikeVerticle = new ABikeVerticle(restABikeAdapter, vertx);

            // Start Kafka consumer for projection updates
            ABikeProjectionUpdatesConsumer consumer =
                new ABikeProjectionUpdatesConsumer(conf, abikeService);

            abikeVerticle.init();
            consumer.init();
        });
    }
}