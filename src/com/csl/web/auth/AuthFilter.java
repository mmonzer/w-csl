package com.csl.web.auth;

import lombok.Getter;
import lombok.Setter;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Service;

import java.util.logging.Logger;

public class AuthFilter implements Filter {

	boolean disabled=true;
	
	@Setter
    @Getter
    private boolean debug=true;;

	
    private static final Logger LOG = Logger.getLogger(AuthFilter.class.getName());

    private static final String TOKEN_PREFIX = "Bearer";
    private static final String LOGIN_ENDPOINT = "/login";
    private static final String REGISTRATION_ENDPOINT = "/registration";
    private static final String HTTP_POST = "POST";

    private final String authEndpointPrefix;

    private final TokenService tokenService;

	private final Service sparkServer;

    public AuthFilter(Service sparkServer,String authEndpointPrefix, TokenService tokenService, boolean d) {
        this.authEndpointPrefix = authEndpointPrefix;
        this.tokenService = tokenService;
        this.debug=d;
        this.sparkServer=sparkServer;
    }

    public void handle(Request request, Response response) {
    	
    	if (debug) System.out.println("Request:"+request.requestMethod()+"   "+request.pathInfo());
    		
    	if (debug) System.out.println("headers="+request.headers());
    	
        if (!isLoginRequest(request) && !isRegistrationRequest(request) && isRequestWithAuthorization(request) && !disabled) {
            String authorizationHeader = request.headers("Authorization");
            if (debug) System.out.println(" Authorization:"+authorizationHeader);
        	
            if (authorizationHeader == null) {
                LOG.warning("Missing Authorization header");
                response.status(404);
                //halt(401);
            } else if (!tokenService.validateToken(authorizationHeader.replace(TOKEN_PREFIX, ""))) {
                LOG.warning("Expired token " + authorizationHeader);
                sparkServer.halt(401);
            }
        }
    }
    
    private boolean isRequestWithAuthorization(Request r) {
    	boolean b= (r.requestMethod().compareToIgnoreCase("options")!=0);
    	if (debug) System.out.println("isRequestWithAuthorization -->"+b);
        return b;
    }

    private boolean isLoginRequest(Request request) {
    	
    	if (debug) System.out.println("isLogin:"+authEndpointPrefix + LOGIN_ENDPOINT);
    	if (debug) System.out.println("    uri:"+request.uri()+" method:"+request.requestMethod()+"  "+request.contextPath());
    	
    	if (debug) System.out.println("  "+request.raw().getContextPath());
    	
       boolean b= request.uri().equals(authEndpointPrefix + LOGIN_ENDPOINT) ; //&& request.requestMethod().equals(HTTP_POST);
       if (debug)  System.out.println("-->"+b);
       return b;
    }

    private boolean isRegistrationRequest(Request request) {
        boolean b=request.uri().equals(authEndpointPrefix + REGISTRATION_ENDPOINT) && request.requestMethod().equals(HTTP_POST);
        if (debug) System.out.println("is RegistrationRequest -->"+b);
        return b;
    }

}
