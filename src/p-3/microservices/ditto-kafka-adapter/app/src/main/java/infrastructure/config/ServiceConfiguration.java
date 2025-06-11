package infrastructure.config;

public class ServiceConfiguration {

    private static ServiceConfiguration instance;


    private final String kafkaBootstrapServers;

    private ServiceConfiguration() {

        this.kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    }

    public static synchronized ServiceConfiguration getInstance() {
        if (instance == null) {
            instance = new ServiceConfiguration();
        }
        return instance;
    }


    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

}