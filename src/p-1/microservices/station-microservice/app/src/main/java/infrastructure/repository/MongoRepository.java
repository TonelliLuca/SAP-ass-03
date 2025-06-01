package infrastructure.repository;

import application.ports.StationRepository;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import domain.model.P2d;
import domain.model.Station;
import infrastructure.config.ServiceConfiguration;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoRepository implements StationRepository {

    private final MongoCollection<Document> collection;

    public MongoRepository(MongoClient mongoClient) {
        ServiceConfiguration config = ServiceConfiguration.getInstance();
        this.collection = mongoClient.getDatabase(config.getMongoDatabase()).getCollection(config.getMongoCollection());
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
        P2d location = new P2d(loc.getDouble("x"), loc.getDouble("y"));
        HashSet<String> dockedBikes = new HashSet<>((java.util.List<String>) doc.get("dockedBikes"));
        Station station = new Station(id, location, capacity);
        dockedBikes.forEach(station::dockBike);
        return station;
    }

    private <T> CompletableFuture<T> toFuture(Publisher<T> publisher) {
        CompletableFuture<T> future = new CompletableFuture<>();
        publisher.subscribe(new org.reactivestreams.Subscriber<>() {
            private T value;
            @Override public void onSubscribe(org.reactivestreams.Subscription s) { s.request(Long.MAX_VALUE); }
            @Override public void onNext(T t) { value = t; }
            @Override public void onError(Throwable t) { future.completeExceptionally(t); }
            @Override public void onComplete() { future.complete(value); }
        });
        return future;
    }

    @Override
    public CompletableFuture<Optional<Station>> findById(String id) {
        Bson filter = Filters.eq("_id", id);
        return toFuture(collection.find(filter).first())
                .thenApply(doc -> doc != null ? Optional.of(documentToStation(doc)) : Optional.empty());
    }

    @Override
    public CompletableFuture<Void> save(Station station) {
        Document doc = stationToDocument(station);
        return toFuture(collection.insertOne(doc)).thenApply(res -> null);
    }

    @Override
    public CompletableFuture<Void> update(Station station) {
        Bson filter = Filters.eq("_id", station.getId());
        Document doc = stationToDocument(station);
        ReplaceOptions options = new ReplaceOptions().upsert(false);
        return toFuture(collection.replaceOne(filter, doc, options)).thenApply(res -> null);
    }

    @Override
    public CompletableFuture<HashSet<Station>> getAll() {
        CompletableFuture<HashSet<Station>> future = new CompletableFuture<>();
        HashSet<Station> stations = new HashSet<>();
        collection.find().subscribe(new org.reactivestreams.Subscriber<>() {
            @Override public void onSubscribe(org.reactivestreams.Subscription s) { s.request(Long.MAX_VALUE); }
            @Override public void onNext(Document doc) { stations.add(documentToStation(doc)); }
            @Override public void onError(Throwable t) { future.completeExceptionally(t); }
            @Override public void onComplete() { future.complete(stations); }
        });
        return future;
    }
}