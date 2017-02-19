package com.andy.rest.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Response {
	private List<Map<String, Object>> objects = new ArrayList<Map<String, Object>>();
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

	public List<Map<String, Object>> getObjects() {
	    return objects;
	}

	public void setObjects(List<Map<String, Object>> objects) {
	    this.objects = objects;
	}

	public long getTime() {
	    return time;
	}

	public void setTime(long time) {
	    this.time = time;
	}

}
