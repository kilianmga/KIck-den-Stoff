package com.kickdenstoff.backend.service;

public class LmStudioException extends RuntimeException {

    public LmStudioException(String message) {
        super(message);
    }

    public LmStudioException(String message, Throwable cause) {
        super(message, cause);
    }
}
