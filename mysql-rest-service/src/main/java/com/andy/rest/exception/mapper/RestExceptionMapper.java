package com.andy.rest.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.andy.rest.exception.RestException;

public class RestExceptionMapper implements ExceptionMapper<RestException> {

    @Override
    public Response toResponse(RestException exception) {
	return Response.status(exception.getResponse().getStatus()).build();
    }

}
