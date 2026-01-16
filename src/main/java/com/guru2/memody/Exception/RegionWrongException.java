package com.guru2.memody.Exception;

public class RegionWrongException extends RuntimeException {
    public RegionWrongException(String message) {
        super(message);
    }
    public RegionWrongException() {
        super("Region Name wrong");
    }
}
