package com.csl.web.auth.user;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mindrot.BCrypt;

public final class UserService {

	private static final String BCRYPT_SALT = BCrypt.gensalt();

    private final List<User> users = new CopyOnWriteArrayList<>();

    public final User register(String userName, String password, String firstName, String lastName) {
        if (users.stream().filter(user -> user.getUsername().equals(userName)).findAny().isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        User u=User.of(userName, password, firstName, lastName, new ArrayList<>());
        users.add(u);
        return u;
    }

    
    public final User register(String userName, String password, String firstName, String lastName, boolean encrypt) {
        if (users.stream().filter(user -> user.getUsername().equals(userName)).findAny().isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        if (encrypt) password =encrypt(password);
        User u=User.of(userName, password, firstName, lastName, new ArrayList<>());
        users.add(u);
        return u;
        
    }
    
    public final User get(String userName) {
    	
        User u= users
                .stream()
                .filter(user -> user.getUsername().equals(userName))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("User does not exist"));
        
        return u;
    }

    public final void update(User user) {
        users.add(user);
    }

	public List<User> getUsers() {
		// TODO Auto-generated method stub
		return users;
	}

	public String encrypt(String pass) {
		
		return BCrypt.hashpw(pass, BCRYPT_SALT);
	}
    
  
}
