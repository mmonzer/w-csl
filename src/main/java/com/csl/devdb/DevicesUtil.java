package com.csl.devdb;

import com.ucsl.interfaces.IIDSLearnedRules;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;


public class DevicesUtil {

	public static Json getLearnedModelTableAsJson(IIDSMainProcessor idsMainProcessor) {

		IIDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile();

		System.out.println(idsLearnedRules);
		System.out.println(idsLearnedRules.toJsonForTable());

		System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForTable()));

		return idsLearnedRules.toJsonForTable();

	}

	public static Json getLearnedModelTableAsJsonDpi(IIDSMainProcessor idsMainProcessor) {

		IIDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile();

		System.out.println(idsLearnedRules);
		System.out.println(idsLearnedRules.toJsonForTableDpi());

		return idsLearnedRules.toJsonForTableDpi();

	}

}