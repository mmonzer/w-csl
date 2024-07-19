package com.csl.web.auth;

import com.csl.web.auth.user.UserService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class AuthentificationManager {
	
	boolean debug=false;

	private static String base="secretjwt123457fshdfqsgcvszejb";

    private static final String SECRET_JWT = base+base+base+base;

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    private final TokenService tokenService = new TokenService(SECRET_JWT);

    private final UserService userService = new UserService();

}
