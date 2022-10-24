package com.csl.web.auth.controller;

//import static spark.Spark.get;
//import static spark.Spark.halt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.csl.intercom.jsoncmd.ApiGetHelp;
import com.csl.web.auth.TokenService;
import com.csl.web.auth.user.Role;
import com.ucsl.json.Json;

import spark.Service;

public class HelpController extends AbstractTokenController {
	
	private boolean debug=false;;

	List<String> apis=new ArrayList<>();
	boolean noprotect=true;
	Service sparkServer=null;

    public HelpController(Service sparkServer,TokenService tokenService, boolean noprotect) {
        super(tokenService);
        this.noprotect=noprotect;
        this.sparkServer=sparkServer;
    }

    public void addApiName(String s) {
    	apis.add(s);
    }
    public boolean isDebug() {
		return debug;
	}



	public void setDebug(boolean debug) {
		this.debug = debug;
	}

    
    public HelpController init() {
    	
    	
    	 // PROTECTED ENDPOINT FOR DEVELOPER ROLE_PROPERTY
        sparkServer.get("/apihelp", (request, response) -> {
            if (noprotect || hasRole(request, new Role[]{Role.DEVELOPER, Role.ADMIN})) {
            	

            	System.out.println(request);
            	Json jj=Json.object();
    			Set<String> paramKeys= request.queryParams();
    			List<String> varNames= new ArrayList<String>(paramKeys);


    			for (String name:varNames) {
    				String[] z = request.queryParamsValues(name);
    				if (z!=null) jj.set(name,z[0]);

    			}

                String s= ""+new ApiGetHelp().getHelp(apis, jj);
                response.type("text/html"); 
                response.body(s);
                return s;
            } else {
            	sparkServer.halt(401);
                return "";
            }
        });
        
        
        
        return this;
    }
}
