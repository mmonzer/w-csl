package com.ucsl.interfaces;

import com.ucsl.json.Json;

public interface IIDSOperationManager extends ICmdHelpProvider{

	Json exec(Json params);

}
