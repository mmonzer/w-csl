package com.csl.web.auth.controller;

import com.csl.web.auth.TokenService;
import com.csl.web.auth.user.Role;

import spark.Service;

//import static spark.Spark.get;
//import static spark.Spark.halt;

public class UserController extends AbstractTokenController {

		
    private boolean debug=false;;

	
Service sparkServer;
	
    public UserController(Service sparkServer,TokenService tokenService, boolean debug) {
        super(tokenService);
        this.sparkServer=sparkServer;
        this.debug=debug;
    }


    public boolean isDebug() {
		return debug;
	}



	public void setDebug(boolean debug) {
		this.debug = debug;
	}

    public UserController init() {
    	 // PROTECTED ENDPOINT FOR DEVELOPER ROLE_PROPERTY
    	sparkServer.get("/protected/developer", (request, response) -> {
            if (hasRole(request, new Role[]{Role.DEVELOPER, Role.ADMIN})) {
                return "PROTECTED RESOURCE FOR DEVELOPER";
            } else {
            	sparkServer.halt(401);
                return "";
            }
        });

        // PROTECTED ENDPOINT FOR MANAGER ROLE_PROPERTY
    	sparkServer.get("/protected/manager", (request, response) -> {
            if (hasRole(request, new Role[]{Role.MANAGER, Role.ADMIN})) {
                return "PROTECTED RESOURCE FOR MANAGER";
            } else {
            	sparkServer.halt(401);
                return "";
            }
        });

        // PROTECTED ENDPOINT FOR ADMIN ROLE_PROPERTY
    	sparkServer.get("/protected/admin", (request, response) -> {
            if (hasRole(request, new Role[]{Role.ADMIN})) {
                return "PROTECTED RESOURCE FOR ADMIN";
            } else {
            	sparkServer.halt(401);
                return "";
            }
        });

        // PROTECTED ENDPOINT FOR ALL ROLES
    	sparkServer.get("/protected/all", (request, response) -> {
            if (hasRole(request, new Role[]{})) {
                return "PROTECTED RESOURCE FOR ALL ROLES";
            } else {
            	sparkServer.halt(401);
                return "";
            }
        });
        
        return this;
    }
}
