package domain.event;

import java.io.Serializable;

public interface Event extends Serializable {
    String getId();
    String getTimestamp();
}
