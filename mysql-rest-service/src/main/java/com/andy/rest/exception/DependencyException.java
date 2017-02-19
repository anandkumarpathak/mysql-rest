package com.andy.rest.exception;

public class DependencyException extends Exception{
    public DependencyException(String message, Throwable root) {
	super(message, root);
    }
}
