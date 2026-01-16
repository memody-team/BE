package com.guru2.memody.Exception;

public class UserNameAlreadyExistsException extends RuntimeException {
    public UserNameAlreadyExistsException() {
        super("Username already exists");
    }
}
