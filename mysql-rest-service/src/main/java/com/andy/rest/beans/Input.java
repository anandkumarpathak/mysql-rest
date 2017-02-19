package com.andy.rest.beans;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Input<T> implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 4854851439989644514L;

   

    private boolean transaction;
    
    private Map<String,List<Map<String, T>>> entityGroups; 
    

//    private List<Map<String, T>> objects;
//
//    public List<Map<String, T>> getObjects() {
//	return this.objects;
//    }
//
//    public void setObjects(List<Map<String, T>> objects) {
//	this.objects = objects;
//    }

    public boolean isTransaction() {
	return this.transaction;
    }

    public void setTransaction(boolean transaction) {
	this.transaction = transaction;
    }

    public Map<String,List<Map<String, T>>> getEntityGroups() {
	return entityGroups;
    }

    public void setEntityGroups(Map<String,List<Map<String, T>>> entityGroups) {
	this.entityGroups = entityGroups;
    }    

    

}