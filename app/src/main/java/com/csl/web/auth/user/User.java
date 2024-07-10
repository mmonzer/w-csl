package com.csl.web.auth.user;

import java.util.List;

public final class User {

    private final String username;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final List<Role> roles;

    private User(String username, String password, String firstName, String lastName, List<Role> roles) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public User assignRole(Role role) {
        if (!roles.contains(role)) {
            roles.add(role);
        }
        return this;
    }

    public User revokeRole(Role role) {
        if (!roles.contains(role)) {
            roles.remove(role);
        }
        return this;
    }

    public static final User of(String username, String password, String firstName, String lastName, List<Role> roles) {
    	   
        return new User(username, password, firstName, lastName, roles);
    }
    
    @Override
    public String toString() {
    	// TODO Auto-generated method stub
    	return "username:"+username+" password:"+password+" firstName:"+firstName+" lastName:"+lastName+" roles:"+roles;
    }
}
