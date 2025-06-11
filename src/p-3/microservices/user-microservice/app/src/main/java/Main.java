import application.UserServiceImpl;
import application.ports.UserEventPublisher;
import application.ports.UserProducerPort;
import application.ports.UserServiceAPI;
import infrastructure.adapter.kafka.RideUpdatesConsumer;
import infrastructure.adapter.kafka.UserUpdatesProducer;
import infrastructure.utils.UserEventPublisherImpl;
import infrastructure.adapter.web.RESTUserAdapter;
import infrastructure.adapter.web.UserVerticle;
import infrastructure.config.ServiceConfiguration;
import infrastructure.persistence.MongoUserRepository;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            String bootstrapServers = config.getKakaConf();

            logger.info("Configuration loaded: " + conf.encodePrettily());
            MongoClient mongoClient = MongoClient.create(vertx, config.getMongoConfig());
            MongoUserRepository repository = new MongoUserRepository(mongoClient);
            UserEventPublisher UserEventPublisher = new UserEventPublisherImpl(vertx);
            UserProducerPort producer = new UserUpdatesProducer(bootstrapServers);
            UserServiceAPI service = new UserServiceImpl(repository, UserEventPublisher, producer);
            RESTUserAdapter controller = new RESTUserAdapter(service, vertx);
            UserVerticle userVerticle = new UserVerticle(controller, vertx);
            RideUpdatesConsumer consumer = new RideUpdatesConsumer(service, bootstrapServers);
            userVerticle.init();
            consumer.init();

            //RideCommunicationAdapter rideAdapter = new RideCommunicationAdapter(service, vertx);
            //userVerticle.init();
            //rideAdapter.init();
        });
    }
}