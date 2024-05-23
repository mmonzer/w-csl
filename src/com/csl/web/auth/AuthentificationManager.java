package com.csl.web.auth;

import com.csl.web.auth.controller.AuthController;
import com.csl.web.auth.controller.HelpController;
import com.csl.web.auth.controller.UserController;
import com.csl.web.auth.user.Role;
import com.csl.web.auth.user.User;
import com.csl.web.auth.user.UserService;
import com.ucsl.json.Json;
import spark.Request;
import spark.Response;
import spark.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AuthentificationManager {
	
	boolean debug=false;

	private static String base="secretjwt123457fshdfqsgcvszejb";

    private static final String SECRET_JWT = base+base+base+base;

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    private final TokenService tokenService = new TokenService(SECRET_JWT);

    private final UserService userService = new UserService();
	
    public void addAuthentification(Service sparkServer) {
    	userService.register("user123", "123456", "John", "Lacey",true).assignRole(Role.INTEGRATOR).assignRole(Role.ADMIN);

        new AuthController(sparkServer, userService, tokenService, debug).init();
        new UserController(sparkServer, tokenService, debug).init();

        HelpController helpController = new HelpController(sparkServer, tokenService, true).init();
        
        // PERIODIC TOKENS CLEAN UP
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            if (debug) System.out.println("Removing expired tokens");
            tokenService.removeExpired();
        }, 60, 60, TimeUnit.SECONDS); // every minute

        for (User u : userService.getUsers()) {
            System.out.println(u);
        }
    }

}
