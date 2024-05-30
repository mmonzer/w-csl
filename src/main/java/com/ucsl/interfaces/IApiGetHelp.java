package com.ucsl.interfaces;

import com.ucsl.json.Json;

import java.util.List;

public interface IApiGetHelp {

	String getHelp(List<String> apiNames, Json params);

	String getHelp(String apiName, Json params);

}