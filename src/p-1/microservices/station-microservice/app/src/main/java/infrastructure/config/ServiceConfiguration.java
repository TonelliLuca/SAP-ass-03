package infrastructure.config;

import com.mongodb.reactivestreams.client.MongoCollection;

public class ServiceConfiguration {

    private static ServiceConfiguration instance;

    private final String mongoConnection;
    private final String mongoDatabase;
    private final String kafkaBootstrapServers;
    private final String mongoCollection;

    private ServiceConfiguration() {
        this.mongoConnection = System.getenv().getOrDefault("MONGO_CONNECTION", "mongodb://localhost:27017");
        this.mongoDatabase = System.getenv().getOrDefault("MONGO_DATABSE", "stations_db");
        this.kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        this.mongoCollection = System.getenv().getOrDefault("MONGO_COLLECTION", "stations");
    }

    public static synchronized ServiceConfiguration getInstance() {
        if (instance == null) {
            instance = new ServiceConfiguration();
        }
        return instance;
    }

    public String getMongoConnection() {
        return mongoConnection;
    }

    public String getMongoDatabase() {
        return mongoDatabase;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public String getMongoCollection() {
        return mongoCollection;
    }
}