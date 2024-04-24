package com.ucsl.interfaces;

import java.util.List;

import com.ucsl.json.Json;

public interface IApiGetHelp {

	String getHelp(List<String> apiNames, List<String> apiDescriptions, Json params);

	String getHelp(String apiName, String apiDescription, Json params);

}