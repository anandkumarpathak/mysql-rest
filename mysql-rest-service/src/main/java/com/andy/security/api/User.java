package com.andy.security.api;

import java.security.Principal;

public class User implements Principal {

    private String uid;
    private String firstName;
    private String lastName;
    private String password;
    private String hash;

    public String getName() {
	return getFirstName() + " " + getLastName();
    }

    public String getHash() {
	return hash;
    }

    public void setHash(String hash) {
	this.hash = hash;
    }

    public String getUid() {
	return uid;
    }

    public void setUid(String uid) {
	this.uid = uid;
    }

    public String getPassword() {
	return password;
    }

    public void setPassword(String password) {
	this.password = password;
    }

    public String getFirstName() {
	return firstName;
    }

    public void setFirstName(String firstName) {
	this.firstName = firstName;
    }

    public String getLastName() {
	return lastName;
    }

    public void setLastName(String lastName) {
	this.lastName = lastName;
    }

    @Override
    public String toString() {
	return "User [uid=" + uid + ", firstName=" + firstName + ", lastName=" + lastName + ", password=" + password + ", hash="
		+ hash + "]";
    }

}
