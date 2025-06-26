package domain.model;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ddd.Entity;

public class User implements Entity<String>, Serializable {
    public enum UserType { ADMIN, USER }

    private final String username;
    private final UserType type;
    private volatile int credit;

    @JsonCreator
    public User(
            @JsonProperty("username") String username,
            @JsonProperty("type") UserType type,
            @JsonProperty("credit") int credit
    ){
        this.username = username;
        this.type = type;
        this.credit = credit;
    }

    public UserType getType() {
        return type;
    }

    public int getCredit() {
        return credit;
    }

    @Override
    public String toString() {
        return String.format("User{username='%s', type='%s', credit=%d}", username, type, credit);
    }

    @Override
    @JsonProperty("username")
    public String getId() {
        return username;
    }
}