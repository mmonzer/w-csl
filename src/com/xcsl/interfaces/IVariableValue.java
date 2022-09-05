package com.xcsl.interfaces;

public interface IVariableValue {

	IVariableValue createBoolean();
	IVariableValue createInteger();
	IVariableValue createDouble();
	IVariableValue createString();
	IVariableValue createJson();


	boolean isBoolean();
	boolean isInteger();
	boolean isDouble();
	boolean isString();

	boolean canBeUsedAsBoolean();	// with type conversion 
	boolean canBeUsedAsInteger();
	boolean canBeUsedAsDouble();
	boolean canBeUsedAsString();
	
	boolean convertAsBoolean();	// return true if conversion ok
	boolean convertAsInteger();
	boolean convertAsDouble();
	boolean convertAsString();

	IVariableValue setValue(double x); //
	IVariableValue setValue(int i); //
	IVariableValue setValue(boolean b); //
	IVariableValue setValue(String s); //
	
	Object getValue();
	
	double  getAsDouble(); //
	int     getAsInteger(); //
	boolean getAsBoolean(); //
	String  getAsString(); //
	
}
