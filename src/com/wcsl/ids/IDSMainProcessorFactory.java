package com.wcsl.ids;

import com.csl.ids.IDSParams;
import com.ucsl.interfaces.ICSLLogger;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.interfaces.IIDSMainProcessorFactory;
import com.ucsl.json.Json;

public class IDSMainProcessorFactory implements IIDSMainProcessorFactory {

	
	public static IDSMainProcessorFactory instance = new IDSMainProcessorFactory();
	
	@Override
	public IIDSMainProcessor createIDSMainProcessor(Json config, String cslConfDir, ICSLLogger logger) {
		// TODO Auto-generated method stub
		IDSMainProcessor idsMainProcessor = new IDSMainProcessor(config, cslConfDir);
		
		idsMainProcessor.setLogger(logger);
		//public IDSMainProcessor(
	//			Json jConfig,
	//			String cslConfDir
		
		return idsMainProcessor;
		
	}

	public static IDSParams createIDSParams(IIDSMainProcessor idsMainProcessor) {
		// TODO Auto-generated method stub
		return new IDSParams(idsMainProcessor);
	}

}
