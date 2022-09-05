package com.xcsl.interfaces;

import java.util.List;

import com.xcsl.json.Json;

public interface IApiGetHelp {

	String getHelp(List<String> apiNames, Json params);

	String getHelp(String apiName, Json params);

}