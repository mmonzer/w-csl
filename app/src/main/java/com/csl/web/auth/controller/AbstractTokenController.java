package com.csl.web.auth.controller;

import com.csl.web.auth.TokenService;
import lombok.Setter;

public abstract class AbstractTokenController {
    @Setter
    private boolean debug = false;

    private static final String TOKEN_PREFIX = "Bearer";

    private final TokenService tokenService;

    public AbstractTokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public boolean isDebug() {
        return debug;
    }
}
