package com.csl.interfaces;

import com.ucsl.interfaces.IResult;

import java.util.Map;

public interface IModule {

	public  IResult init(ICSLContext context,IModuleContext mcontext);
	public  IResult start(ICSLContext context,IModuleContext mcontext);// reload config

	IResult restart(ICSLContext context, IModuleContext mcontext);

	public  IResult stop(ICSLContext context, IModuleContext mcontext);
	
	public  IResult execInputPart(ICSLContext context, IModuleContext mcontext);
	public  IResult execStepPart(ICSLContext context,IModuleContext mcontext);
	public  IResult execOutputPart(ICSLContext context,IModuleContext mcontext);

	IResult execCommand(ICSLContext context, IModuleContext mcontext, Map<String, String> params);
}

