package application.port;

public interface DittoTranslatorServicePort {
    void handleEvent(Object event);

    void sendDittoResponse(String thingId, String correlationId, String ok, Object o, String replyTarget);

}
