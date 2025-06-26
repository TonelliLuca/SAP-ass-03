package application.ports;

 import domain.model.User;
 import domain.event.Event;
 import java.util.List;
 import java.util.Optional;
 import java.util.concurrent.CompletableFuture;

 /**
  * Port for the User Service API.
  * Provides methods to manage the Application domain.
  */
 public interface UserServiceAPI {

     /**
      * Signs in a user with the given username.
      *
      * @param event the event containing user sign-in details
      * @return a CompletableFuture containing the user details as a User object if the sign-in is successful
      */
     CompletableFuture<User> signIn(Event event);

     /**
      * Signs up a new user with the given username and type.
      *
      * @param event the event containing user sign-up details
      * @return a CompletableFuture containing the created user details as a User object
      */
     CompletableFuture<User> signUp(Event event);

     /**
      * Retrieves a user by its username.
      *
      * @param username the username of the user
      * @return a CompletableFuture containing an Optional with the user as a User object if found, or an empty Optional if not found
      */
     CompletableFuture<Optional<User>> getUserByUsername(String username);

     /**
      * Updates the details of an existing user.
      *
      * @param event the event containing updated user details
      * @return a CompletableFuture containing the updated user as a User object
      */
     CompletableFuture<User> updateUser(Event event);

     /**
      * Recharges the credit of a user.
      *
      * @param event the event containing credit recharge details
      * @return a CompletableFuture containing the updated user as a User object
      */
     CompletableFuture<User> rechargeCredit(Event event);

     /**
      * Retrieves all users.
      *
      * @return a CompletableFuture containing a List of all users
      */
     CompletableFuture<List<User>> getAllUsers();

     /**
      * Initializes the User Service API.
      * This method can be used to set up necessary configurations or resources.
      */
     void init();

     CompletableFuture<Void> abikeRequested(Event event);

 }