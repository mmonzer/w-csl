package com.ucsl.interfaces;

import com.ucsl.json.Json;

public interface IIDSMainProcessorFactory {

	IIDSMainProcessor createIDSMainProcessor(Json config, String cslConfDir, ICSLLogger logger );
}
