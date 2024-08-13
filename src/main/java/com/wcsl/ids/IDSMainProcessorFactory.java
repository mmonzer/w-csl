package com.wcsl.ids;

import com.csl.core.Config;
import com.csl.ids.IDSParams;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.interfaces.IIDSMainProcessorFactory;

public class IDSMainProcessorFactory implements IIDSMainProcessorFactory {

	
	public static IDSMainProcessorFactory instance = new IDSMainProcessorFactory();

	@Override
	public IIDSMainProcessor createIDSMainProcessor(Config.IdsConf config, String cslConfDir) {
		IDSMainProcessor idsMainProcessor = new IDSMainProcessor(config, cslConfDir);
		return idsMainProcessor;

	}

	public static IDSParams createIDSParams(IIDSMainProcessor idsMainProcessor) {
		// TODO Auto-generated method stub
		return new IDSParams(idsMainProcessor);
	}

}
