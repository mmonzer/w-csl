package com.ucsl.interfaces;

import com.csl.core.Config;

public interface IIDSMainProcessorFactory {

	IIDSMainProcessor createIDSMainProcessor(Config.IdsConf config, String cslConfDir);
}
