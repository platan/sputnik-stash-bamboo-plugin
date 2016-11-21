package com.github.platan.bamboo.sputnik;


public class StashException extends RuntimeException {

    public StashException() {
    }

    public StashException(Throwable cause) {
        super(cause);
    }

    public StashException(String message) {
        super(message);
    }
}
