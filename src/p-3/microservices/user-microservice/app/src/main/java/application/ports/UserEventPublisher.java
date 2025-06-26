package application.ports;

import domain.event.Event;

/**
* Interface representing an event publisher for user updates.
* This port is responsible for publishing user-related events to external systems or services.
*/
public interface UserEventPublisher {

 /**
  * Publishes an update for a specific user.
  *
  * @param username the username of the user whose update is being published.
  * @param event the event containing the user update details.
  */
 void publishUserUpdate(String username, Event event);

 /**
  * Publishes updates for all users.
  *
  * @param event the event containing updates for all users.
  */
 void publishAllUsersUpdates(Event event);

}