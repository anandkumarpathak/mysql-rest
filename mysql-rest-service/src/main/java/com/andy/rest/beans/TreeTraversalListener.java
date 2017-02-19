package com.andy.rest.beans;

public interface TreeTraversalListener {
    public void childernTraversed(String entity, int numberOfChildren);
    public void childFound(String entity, String childEntity, String foreignKeyName);
}