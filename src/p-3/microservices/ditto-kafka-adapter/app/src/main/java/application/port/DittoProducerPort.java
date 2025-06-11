package application.port;

public interface DittoProducerPort {
    void send(String key, String dittoJson);
    void sendWithCorrelationHeader(String key, String dittoJson);
}