package com.example.kproject.exception;

public class ChatUploadException extends RuntimeException {

    public ChatUploadException(String message) {
        super(message);
    }

    public ChatUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
