package com.wcsl.ids;

import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.interfaces.IIDSOperationManager;
import com.ucsl.interfaces.IIDSOperationManagerFactory;

public class IDSOperationManagerFactory implements IIDSOperationManagerFactory {

	public static IDSOperationManagerFactory instance= new IDSOperationManagerFactory();
	
	@Override
	public IIDSOperationManager createIDSOperationManagerFactory(IIDSMainProcessor idsMainProcessor) {
		// TODO Auto-generated method stub
		
		//	//IDSOperationManager opManager= new IDSOperationManager(CSLContext.instance.getIDSMainProcessor());
		
		return new IDSOperationManager(idsMainProcessor);
	}

	

}
