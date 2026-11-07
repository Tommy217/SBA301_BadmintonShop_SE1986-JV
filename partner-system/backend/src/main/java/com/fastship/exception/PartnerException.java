package com.fastship.exception;

import org.springframework.http.HttpStatus;

public class PartnerException extends RuntimeException {
    private final HttpStatus status;

    public PartnerException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static PartnerException notFound(String message) { return new PartnerException(HttpStatus.NOT_FOUND, message); }
    public static PartnerException badRequest(String message) { return new PartnerException(HttpStatus.BAD_REQUEST, message); }
    public static PartnerException conflict(String message) { return new PartnerException(HttpStatus.CONFLICT, message); }
}
