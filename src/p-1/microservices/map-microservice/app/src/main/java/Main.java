import application.RestMapServiceAPIImpl;
import application.ports.EventPublisher;
import application.ports.RestMapServiceAPI;
import infrastructure.utils.EventPublisherImpl;
import infrastructure.adapter.web.MapServiceVerticle;
import infrastructure.config.ServiceConfiguration;
import io.vertx.core.Vertx;
import infrastructure.adapter.kafka.MapEventConsumer;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ServiceConfiguration config = ServiceConfiguration.getInstance(vertx);
        config.load().onSuccess(conf -> {
            System.out.println("Configuration loaded: " + conf.encodePrettily());
            EventPublisher eventPublisher = new EventPublisherImpl(vertx);
            RestMapServiceAPI service = new RestMapServiceAPIImpl(eventPublisher);
            MapServiceVerticle mapServiceVerticle = new MapServiceVerticle(service, vertx);
            String bootstrapServers = config.getKakaConf();
            MapEventConsumer consumer = new MapEventConsumer(service, bootstrapServers, "http://schema-registry:8081");
            mapServiceVerticle.init();
            consumer.init();
        });








    }
}