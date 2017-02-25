package com.andy.security.api;

public enum Role {

    Unauthenticated(0), User(1), Approver(2), Editor(3), Manager(4), Administrator(5), Root(6);

    final int level;    

    private Role(final int level) {
	this.level = level;
    }

    public int getLevel() {
	return level;
    }

    public static Role fromLevel(int level) {
	for (Role role : Role.values()) {
	    if (role.getLevel() == level) {
		return role;
	    }
	}
	return null;
    }

    public static Role fromName(String name) {
	for (Role role : Role.values()) {
	    if (role.name().equalsIgnoreCase(name)) {
		return role;
	    }
	}
	return null;
    }

    /**
     * Finds if the current role is higher than given role
     * @param role
     * @return
     */
    public boolean isHigher(Role role) {
	return this.getLevel() > role.getLevel();
    }

    /**
     * Finds if the current role is compatible with the given role.
     * Role is normally compatible if its higher or same as the given role
     * @param role
     * @return
     */
    public boolean isCompatible(Role role) {
	return this.getLevel() >= role.getLevel();
    }
    
    public String getName() {
	return this.name();
    }

    @Override
    public String toString() {
	return "Role[ '" + this.name() + "','" + this.level + "')";
    }
}
