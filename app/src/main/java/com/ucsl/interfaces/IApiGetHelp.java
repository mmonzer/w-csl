package com.ucsl.interfaces;

import java.util.List;

import com.ucsl.json.Json;

public interface IApiGetHelp {

	String getHelp(List<String> apiNames, Json params);

	String getHelp(String apiName, Json params);

}