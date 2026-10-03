package com.example.demo.model;

public record DatabaseHealth(State state, String message) {
    public enum State { CONNECTED, NOT_CONFIGURED, UNAVAILABLE }
    public boolean isConnected() { return state == State.CONNECTED; }
    public String getState() { return state.name(); }
    public String getMessage() { return message; }
}
