package com.ucsl.interfaces;

import com.csl.core.Config;
import com.ucsl.json.Json;

public interface IIDSMainProcessorFactory {

	IIDSMainProcessor createIDSMainProcessor(Config.IdsConf config, String cslConfDir);
}
