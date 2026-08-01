package com.example.techfix;

public class User {

    private final long id;
    private final String fullName;
    private final String email;

    public User(long id, String fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }
}
