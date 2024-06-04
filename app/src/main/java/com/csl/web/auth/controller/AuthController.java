package com.csl.web.auth.controller;

import com.csl.web.auth.AuthFilter;
import com.csl.web.auth.TokenService;
import com.csl.web.auth.user.Role;
import com.csl.web.auth.user.User;
import com.csl.web.auth.user.UserService;
import com.ucsl.json.Json;
import lombok.Getter;
import org.mindrot.jbcrypt.BCrypt;
import spark.Request;
import spark.Response;
import spark.Service;

import java.io.IOException;
import java.util.stream.Collectors;

public class AuthController extends AbstractTokenController {
	
	
	@Getter
    public boolean debug=true;

    private static final String ROLE_PROPERTY = "role";
    private static final String TOKEN_PREFIX = "Bearer";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_NAME_PROPERTY = "username";
    private static final String FIRST_NAME_PROPERTY = "firstName";
    private static final String LAST_NAME_PROPERTY = "lastName";
    private static final String PASSWORD_PROPERTY = "password";
    private static final String AUTH_ENDPOINT_PREFIX = "/auth";

    private static final String BCRYPT_SALT = BCrypt.gensalt();
    private final UserService userService;
    private final TokenService tokenService;

	private AuthFilter authFilter;
	
	Service sparkServer;

    public AuthController(Service sparkServer,UserService userService, TokenService tokenService, boolean debug) {
        super(tokenService);
       // this.gson = gson;
        this.userService = userService;
        this.tokenService = tokenService;
        this.debug=debug;
        this.sparkServer=sparkServer;
    }


    public void setDebug(boolean debug) {
		this.debug = debug;
		authFilter.setDebug(debug);
	}



	public AuthController init() {
        createAdminUser();

        authFilter=new AuthFilter(sparkServer,AUTH_ENDPOINT_PREFIX, tokenService,debug);
        // AUTH FILTER
        sparkServer.before(authFilter);

        // REGISTRATION ENDPOINT
        sparkServer.post(AUTH_ENDPOINT_PREFIX + "/registration", (request, response) -> register(request, response));

        // LOGIN ENDPOINT
        sparkServer.post(AUTH_ENDPOINT_PREFIX + "/login", (request, response) -> login(request, response));

        // LOGOUT ENDPOINT
        sparkServer.post(AUTH_ENDPOINT_PREFIX + "/logout", (request, response) -> logout(request));

        // REFRESH ENDPOINT
        sparkServer.post(AUTH_ENDPOINT_PREFIX + "/token", (request, response) -> refresh(request, response));

        // ME ENDPOINT
        sparkServer.get(AUTH_ENDPOINT_PREFIX + "/me", (request, response) -> me(request, response));

        // ASSIGN ROLE_PROPERTY
        sparkServer.post(AUTH_ENDPOINT_PREFIX + "/roles", (request, response) -> assignRole(request));

        // REVOKE ROLE_PROPERTY
        sparkServer.delete(AUTH_ENDPOINT_PREFIX + "/roles", (request, response) -> revokeRole(request));
        
        return this;

    }

    private String revokeRole(Request request) throws IOException {
        if (hasRole(request, new Role[]{Role.ADMIN})) {
            String json = request.raw().getReader().lines().collect(Collectors.joining());
            //JsonObject jsonRequest = this.gson.fromJson(json, JsonObject.class);
            Json jsonRequest=Json.read(json);
            if (jsonRequest.has(USER_NAME_PROPERTY) && jsonRequest.has(ROLE_PROPERTY)) {
                Role role = Role.valueOf(jsonRequest.get(ROLE_PROPERTY).asString());
                if (role != null) {
                    User user = this.userService.get(jsonRequest.get(USER_NAME_PROPERTY).asString());
                    if (user != null) {
                        user.revokeRole(role);
                        this.userService.update(user);
                    }
                }
            }
        } else {
        	sparkServer.halt(401);
        }

        return "";
    }

    private String assignRole(Request request) throws IOException {
        if (hasRole(request, new Role[]{Role.ADMIN})) {
            String json = request.raw().getReader().lines().collect(Collectors.joining());
            //JsonObject jsonRequest = gson.fromJson(json, JsonObject.class);
            
            Json jsonRequest=Json.read(json);
             
            
            if (jsonRequest.has(USER_NAME_PROPERTY) && jsonRequest.has(ROLE_PROPERTY)) {
                Role role = Role.valueOf(jsonRequest.get(ROLE_PROPERTY).asString());
                if (role != null) {
                    User user = userService.get(jsonRequest.get(USER_NAME_PROPERTY).asString());
                    if (user != null) {
                        user.assignRole(role);
                        userService.update(user);
                    }
                }
            }
        } else {
        	sparkServer.halt(401);
        }

        return "";
    }

    private String me(Request request, Response response) {
        response.type("application/json");
        String userName = getUserNameFromToken(request);
        User user = userService.get(userName);
        Json userJson = Json.object();
        
        userJson.set(USER_NAME_PROPERTY, user.getUsername());
        userJson.set(FIRST_NAME_PROPERTY, user.getFirstName());
        userJson.set(LAST_NAME_PROPERTY, user.getLastName());
        return userJson.toString();
    }

    private String refresh(Request request, Response response) {
        String authorizationHeader = request.headers(AUTHORIZATION_HEADER);
        String token = authorizationHeader.replace(TOKEN_PREFIX, "");
        String userName = getUserNameFromToken(request);
        tokenService.revokeToken(token);
        String refreshedToken = tokenService.newToken(userService.get(userName));
        response.header(AUTHORIZATION_HEADER, TOKEN_PREFIX + " " + refreshedToken);
        return "";
    }

    private String logout(Request request) {
    	System.out.println("Logout ");
        
        String authorizationHeader = request.headers(AUTHORIZATION_HEADER);
        if (authorizationHeader!=null) tokenService.revokeToken(authorizationHeader.replace(TOKEN_PREFIX, ""));
        return "";
    }
    
    
    private Json getUserRoles(User user) {
    	Json j=Json.array();
    	for (Role r:user.getRoles()) {
    		j.add(r.name().toUpperCase());
    		
    	}
    	
    	return j;
    }

    private String login(Request request, Response response) throws IOException {
    	System.out.println("Login :");
        String json = request.raw().getReader().lines().collect(Collectors.joining());
       // JsonObject jsonRequest = gson.fromJson(json, JsonObject.class);
        
        System.out.println(json);
        if (json==null) return "";
        if (json.isEmpty()) return "";
        
        Json jsonRequest=Json.read(json);
        
        System.out.println("login user ="+jsonRequest);
        if (validatePost(jsonRequest)) {
            try {
            	
                String encryptedPassword = BCrypt.hashpw(jsonRequest.get(PASSWORD_PROPERTY).asString(), BCRYPT_SALT);
                User user = userService.get(jsonRequest.get(USER_NAME_PROPERTY).asString());
                //if (user.getPassword().equals(encryptedPassword)) 
                System.out.println("User :"+user);
                if (BCrypt.checkpw(jsonRequest.get(PASSWORD_PROPERTY).asString(), user.getPassword()))
                {
                	System.out.println("User ok :"+user);
                	String token ="new_token"; //tokenService.newToken(user);
                	token =tokenService.newToken(user);
                	System.out.println(" --> token :"+token);
                    response.header(AUTHORIZATION_HEADER, TOKEN_PREFIX + " " + token);
                    Json j= Json.object();
                    j.set("username",user.getUsername());
                    j.set("token",token );
                    j.set("roles",getUserRoles(user));
                    return j.toString();
                }
                else {
                	System.out.println("invalid password");
                }
            } catch (Exception e) {
            	//System.out.println(e);
                response.status(401);
            }
        }
        return "";
    }

    private String register(Request request, Response response) throws IOException {
        String json = request.raw().getReader().lines().collect(Collectors.joining());
        //JsonObject jsonRequest = gson.fromJson(json, JsonObject.class);
        
        Json jsonRequest=Json.read(json);
        
        
        try {
            if (validatePost(jsonRequest)) {
                userService.register(jsonRequest.get(USER_NAME_PROPERTY).asString(),
                        BCrypt.hashpw(jsonRequest.get(PASSWORD_PROPERTY).asString(), BCRYPT_SALT),
                        jsonRequest.has(FIRST_NAME_PROPERTY) ? jsonRequest.get(FIRST_NAME_PROPERTY).asString() : null,
                        jsonRequest.has(LAST_NAME_PROPERTY) ? jsonRequest.get(LAST_NAME_PROPERTY).asString() : null);
                return "";
            } else {
                response.status(400);
            }
        } catch (IllegalArgumentException e) {
            response.status(400);
        }
        return "";
    }

    private void createAdminUser() {
        userService.register("admin", BCrypt.hashpw("admin", BCRYPT_SALT), null, null); //ADMIN USER
        User admin = userService.get("admin");
        admin.assignRole(Role.ADMIN);
        userService.update(admin);
    }

    private boolean validatePost(Json jsonRequest) {
        return jsonRequest != null && jsonRequest.has(USER_NAME_PROPERTY) && jsonRequest.has(PASSWORD_PROPERTY);
    }

}
