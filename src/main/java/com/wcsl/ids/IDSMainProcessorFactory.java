package com.wcsl.ids;

import com.csl.core.Config;

public class IDSMainProcessorFactory {
	public static IDSMainProcessorFactory instance = new IDSMainProcessorFactory();

	public IDSMainProcessor createIDSMainProcessor(Config.IdsConf config, String cslConfDir) {
		return new IDSMainProcessor(config, cslConfDir);

	}

}
