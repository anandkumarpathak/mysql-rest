package com.andy.rest.exception;

public class EntityException extends Exception{
    public EntityException(String message, Throwable root) {
	super(message,root);
    }
}
