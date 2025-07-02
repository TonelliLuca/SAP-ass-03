import application.EBikeServiceImpl;
import application.ports.EbikeProducerPort;
import infrastructure.adapter.kafka.EbikeUpdatesProducer;
import infrastructure.adapter.kafka.RideUpdatesConsumer;
import infrastructure.adapter.web.EBikeVerticle;
import infrastructure.adapter.web.RESTEBikeAdapter;
import infrastructure.config.ServiceConfiguration;
import infrastructure.persistence.MongoEBikeRepository;
import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            System.out.println("Configuration loaded: " + conf.encodePrettily());
            MongoClient mongoClient = MongoClient.create(vertx, config.getMongoConfig());
            MongoEBikeRepository repository = new MongoEBikeRepository(mongoClient);
            String bootstrapServers = config.getKakaConf();
            EbikeProducerPort producer = new EbikeUpdatesProducer(bootstrapServers, "http://schema-registry:8091");
            //MapCommunicationAdapter mapCommunicationAdapter = new MapCommunicationAdapter(vertx);
            EBikeServiceImpl service = new EBikeServiceImpl(repository, producer);
            RESTEBikeAdapter restEBikeAdapter = new RESTEBikeAdapter(service);
            RideUpdatesConsumer consumer = new RideUpdatesConsumer(service, bootstrapServers, "http://schema-registry:8091");
            //RideCommunicationAdapter rideCommunicationAdapter = new RideCommunicationAdapter(service, vertx); // Port for RideCommunicationAdapter
            EBikeVerticle eBikeVerticle = new EBikeVerticle(restEBikeAdapter, vertx);
            //rideCommunicationAdapter.init();
            eBikeVerticle.init();
            consumer.init();
        });

    }
}