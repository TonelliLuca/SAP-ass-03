package application;

import application.ports.EBikeRepository;
import application.ports.EBikeServiceAPI;
import application.ports.EbikeProducerPort;
import domain.event.*;
import domain.model.EBike;
import domain.model.EBikeState;
import domain.model.P2d;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EBikeServiceImpl implements EBikeServiceAPI {

    private final EBikeRepository repository;
    private final EbikeProducerPort producer;

    public EBikeServiceImpl(EBikeRepository repository, EbikeProducerPort producer) {
        this.repository = repository;
        this.producer = producer;

        repository.findAll().thenAccept(ebikes -> {
            ebikes.forEach(e -> producer.sendUpdate(new EBikeUpdateEvent(e)));
        });
    }

    @Override
    public CompletableFuture<EBike> createEBike(Event event) {
        if (!(event instanceof EBikeCreateEvent e)) {
            CompletableFuture<EBike> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Event type mismatch"));
            return future;
        }
        EBike ebike = new EBike(
                e.ebikeId(),
                new P2d(e.x(), e.y()),
                EBikeState.AVAILABLE,
                100
        );
        return repository.save(ebike)
                .thenApply(v -> {
                    producer.sendUpdate(new EBikeUpdateEvent(ebike));
                    return ebike;
                });
    }

    @Override
    public CompletableFuture<EBike> rechargeEBike(Event event) {
        if (!(event instanceof EBikeRechargeEvent e)) {
            CompletableFuture<EBike> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Event type mismatch"));
            return future;
        }
        String ebikeId = e.ebikeId();
        return repository.findById(ebikeId)
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        CompletableFuture<EBike> f = new CompletableFuture<>();
                        f.completeExceptionally(new RuntimeException("eBike not found"));
                        return f;
                    }
                    EBike ebike = opt.get();
                    EBike updated = new EBike(
                            ebike.getId(),
                            ebike.getLocation(),
                            EBikeState.AVAILABLE,
                            100
                    );
                    return repository.update(updated).thenApply(v -> {
                        producer.sendUpdate(new EBikeUpdateEvent(updated));
                        return updated;
                    });
                });
    }

    @Override
    public CompletableFuture<EBike> updateEBike(Event event) {
        if (!(event instanceof RequestEBikeUpdateEvent e)) {
            CompletableFuture<EBike> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Event type mismatch"));
            return future;
        }
        EBike update = new EBike(
            e.bikeId(),
            new P2d(e.bikeX(), e.bikeY()),
            EBikeState.valueOf(e.bikeState()),
            e.bikeBattery()
        );
        return repository.findById(e.bikeId())
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        CompletableFuture<EBike> f = new CompletableFuture<>();
                        f.completeExceptionally(new RuntimeException("eBike not found"));
                        return f;
                    }
                    EBike ebike = opt.get();
                    int battery = update.getBatteryLevel();
                    EBikeState state = battery == 0 ? EBikeState.MAINTENANCE : update.getState();
                    EBike updated = new EBike(
                            ebike.getId(),
                            new P2d(update.getLocation().getX(), update.getLocation().getY()),
                            state,
                            battery
                    );
                    return repository.update(updated).thenApply(v -> {
                        producer.sendUpdate(new EBikeUpdateEvent(updated));
                        return updated;
                    });
                });
    }

    @Override
    public CompletableFuture<List<EBike>> getAllEBikes() {
        return repository.findAll();
    }
}
