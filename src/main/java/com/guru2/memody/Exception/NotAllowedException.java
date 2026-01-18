package com.guru2.memody.Exception;

public class NotAllowedException extends RuntimeException {
    public NotAllowedException(String message) {
        super(message);
    }
    public NotAllowedException() { super("Not allowed exception"); }
}
