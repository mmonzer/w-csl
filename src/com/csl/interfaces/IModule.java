package com.csl.interfaces;

import java.util.Map;

import com.xcsl.interfaces.IResult;

public interface IModule {

	public  IResult init(ICSLContext context,IModuleContext mcontext);
	public  IResult start(ICSLContext context,IModuleContext mcontext);
	public  IResult restart(ICSLContext context,IModuleContext mcontext);  // reload config
	public  IResult stop(ICSLContext context,IModuleContext mcontext);
	
	public  IResult execInputPart(ICSLContext context, IModuleContext mcontext);
	public  IResult execStepPart(ICSLContext context,IModuleContext mcontext);
	public  IResult execOutputPart(ICSLContext context,IModuleContext mcontext);

	public IResult execCommand(ICSLContext context,IModuleContext mcontext,Map<String, String> params);
	
	
	
}

