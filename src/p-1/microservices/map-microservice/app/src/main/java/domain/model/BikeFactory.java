package domain.model;

import ddd.Factory;

public class BikeFactory implements Factory {

    private static BikeFactory instance;

    public static BikeFactory getInstance() {
      if (instance == null) {
          instance = new BikeFactory();
      }
      return instance;
    }

    private BikeFactory() {
    }

    public EBike createEBike(String id, float x, float y
                            , BikeState state
                            , int battery
    ) {
      return new EBike(id, new P2d(x, y), state, battery);
    }

    public ABike createABike(String id, float x, float y
          , BikeState state
          , int battery
    ) {
      return new ABike(id, new P2d(x, y), battery, state);
    }

}