package com.andy.rest.beans;

public class Response {
	private Object objects = null;
	private boolean status = true;
	private String message = "Successful";
	private long time = 0;

	public boolean isStatus() {
	    return status;
	}

	public void setStatus(boolean status) {
	    this.status = status;
	}

	public String getMessage() {
	    return message;
	}

	public void setMessage(String message) {
	    this.message = message;
	}

	public Object getObjects() {
	    return objects;
	}

	public void setObjects(Object objects) {
	    this.objects = objects;
	}

	public long getTime() {
	    return time;
	}

	public void setTime(long time) {
	    this.time = time;
	}

}
