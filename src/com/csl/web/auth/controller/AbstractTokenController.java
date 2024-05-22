package com.csl.web.auth.controller;

import com.csl.web.auth.TokenService;
import com.csl.web.auth.user.Role;
import com.csl.web.auth.user.UserPrincipal;
import spark.Request;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractTokenController {
	  private boolean debug=false;;

    private static final String TOKEN_PREFIX = "Bearer";

    private final TokenService tokenService;

    public AbstractTokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    

    public boolean isDebug() {
		return debug;
	}



	public void setDebug(boolean debug) {
		this.debug = debug;
	}



	protected UserPrincipal getUserPrincipal(Request request) {
        String authorizationHeader = request.headers("Authorization");
        String token = authorizationHeader.replace(TOKEN_PREFIX, "");
        return tokenService.getUserPrincipal(token);
    }

    protected boolean hasRole(Request request, Role[] roles) {
        if (roles.length == 0) {
            return true;
        }
        List<Role> userRoles = getUserPrincipal(request).getRoles();
        return userRoles.stream().filter(Arrays.asList(roles)::contains).findAny().isPresent();
    }

    protected String getUserNameFromToken(Request request) {
        String authorizationHeader = request.headers("Authorization");
        String token = authorizationHeader.replace(TOKEN_PREFIX, "");
        return tokenService.getUserPrincipal(token).getUserName();
    }

}
