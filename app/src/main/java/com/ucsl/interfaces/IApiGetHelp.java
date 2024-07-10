package com.ucsl.interfaces;

<<<<<<< HEAD
import java.util.List;

import com.ucsl.json.Json;

public interface IApiGetHelp {

	String getHelp(List<String> apiNames, List<String> apiDescriptions, Json params);

	String getHelp(String apiName, String apiDescription, Json params);
=======
import com.ucsl.json.Json;

import java.util.List;

public interface IApiGetHelp {

	String getHelp(List<String> apiNames, Json params);

	String getHelp(String apiName, Json params);
>>>>>>> origin/feature/refactor_code

}