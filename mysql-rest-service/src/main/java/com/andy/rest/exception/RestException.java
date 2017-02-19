package com.andy.rest.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class RestException extends WebApplicationException {

    public RestException(String message, Response.Status status) {
	super(message, status);
    }
}
