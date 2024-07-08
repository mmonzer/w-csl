package com.csl.web.auth.controller;

import com.csl.web.auth.TokenService;

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


}
