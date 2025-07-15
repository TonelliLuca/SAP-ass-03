package it.unibo;

import domain.model.P2d;
import infrastructure.adapter.persistence.BikeRepositoryImpl;
import domain.model.EBike;
import domain.model.BikeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class EBikeUnitRepositoryTest {
    private BikeRepositoryImpl repository;
    private EBike bike1;
    private EBike bike2;

    @BeforeEach
    public void setUp() {
        repository = new BikeRepositoryImpl();

        bike1 = new EBike("Bike1", new P2d(0,0), BikeState.AVAILABLE, 100);
        bike2 = new EBike("Bike1", new P2d(10,10), BikeState.AVAILABLE, 100);
    }

    @Test
    public void testSaveAndGetBike() throws ExecutionException, InterruptedException {
        repository.saveBike(bike1).get();

        EBike retrievedBike = repository.getBike("Bike1").get();
        assertNotNull(retrievedBike);
        assertEquals("Bike1", retrievedBike.getBikeName());
        assertEquals(BikeState.AVAILABLE, retrievedBike.getState());
    }

    @Test
    public void testGetBike_NotFound() {
        assertThrows(ExecutionException.class, () -> repository.getBike("NonExistentBike").get());
    }

    @Test
    public void testAssignBikeToUser() throws ExecutionException, InterruptedException {
        repository.saveBike(bike1).get();
        repository.assignBikeToUser("User1", bike1).get();

        String assignedUser = repository.isBikeAssigned(bike1).get();
        assertEquals("User1", assignedUser);
    }

    @Test
    public void testUnassignBikeFromUser() throws ExecutionException, InterruptedException {
        repository.saveBike(bike1).get();
        repository.assignBikeToUser("User1", bike1).get();

        repository.unassignBikeFromUser("User1", bike1).get();
        String assignedUser = repository.isBikeAssigned(bike1).get();
        assertNull(assignedUser);
    }

    @Test
    public void testGetAvailableBikes() throws ExecutionException, InterruptedException {
        repository.saveBike(bike1).get();
        repository.saveBike(bike2).get();

        List<EBike> availableBikes = repository.getAvailableBikes().get();
        assertEquals(1, availableBikes.size());
        assertEquals("Bike1", availableBikes.getFirst().getBikeName());
    }
}
