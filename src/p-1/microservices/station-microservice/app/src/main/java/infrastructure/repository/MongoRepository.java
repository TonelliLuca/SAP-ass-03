package infrastructure.repository;

import application.ports.StationRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import domain.model.P2d;
import domain.model.Station;
import infrastructure.config.ServiceConfiguration;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoRepository implements StationRepository {
    private static final Logger log = LoggerFactory.getLogger(MongoRepository.class);

    private final MongoCollection<Document> collection;

    public MongoRepository(MongoClient mongoClient) {
        ServiceConfiguration config = ServiceConfiguration.getInstance();
        this.collection = mongoClient.getDatabase(config.getMongoDatabase())
                                    .getCollection(config.getMongoCollection());
    }

    private Document stationToDocument(Station station) {
        Document doc = new Document("_id", station.getId())
                .append("capacity", station.getCapacity())
                .append("location", new Document("x", station.getLocation().x()).append("y", station.getLocation().y()))
                .append("dockedBikes", station.getDockedBikes());
        return doc;
    }

    private Station documentToStation(Document doc) {
        String id = doc.getString("_id");
        int capacity = doc.getInteger("capacity");
        Document loc = (Document) doc.get("location");
        Number x = loc.get("x", Number.class);
        Number y = loc.get("y", Number.class);
        P2d location = new P2d(x.doubleValue(), y.doubleValue());
        java.util.List<String> dockedBikesList = (java.util.List<String>) doc.get("dockedBikes");
        Station station = new Station(id, location, capacity);
        if (dockedBikesList != null) {
            for (String bikeId : dockedBikesList) {
                try {
                    station.dockBike(bikeId);
                } catch (Exception e) {
                    log.warn("Failed to dock bike {} at station {}: {}", bikeId, id, e.getMessage());
                }
            }
        }
        log.info("Station {} has been created", station.getId());
        return station;
    }

    @Override
    public CompletableFuture<Optional<Station>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            Bson filter = Filters.eq("_id", id);
            Document doc = collection.find(filter).first();
            return doc != null ? Optional.of(documentToStation(doc)) : Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> save(Station station) {
        return CompletableFuture.runAsync(() -> {
            Document doc = stationToDocument(station);
            collection.insertOne(doc);
            log.info("Station {} has been saved", station.getId());
        });
    }

    @Override
    public CompletableFuture<Void> update(Station station) {
        return CompletableFuture.runAsync(() -> {
            Bson filter = Filters.eq("_id", station.getId());
            Document doc = stationToDocument(station);
            ReplaceOptions options = new ReplaceOptions().upsert(false);
            collection.replaceOne(filter, doc, options);
            log.info("Station {} has been updated", station.getId());
        });
    }

    @Override
    public CompletableFuture<HashSet<Station>> getAll() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("MongoRepository.getAll() - Querying all stations from MongoDB");
            HashSet<Station> stations = new HashSet<>();

            collection.find().forEach(doc -> {
                log.info("MongoRepository.getAll() - Found document: {}", doc.toJson());
                stations.add(documentToStation(doc));
            });

            log.info("MongoRepository.getAll() - Completed. Total stations: {}", stations.size());
            return stations;
        });
    }
}