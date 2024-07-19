package com.ucsl.interfaces;

import com.ucsl.json.Json;

import java.util.List;

public interface IApiGetHelp {

	String getHelp(List<String> apiNames, List<String> apiDescriptions, Json params);

	String getHelp(String apiName, String apiDescription, Json params);

}