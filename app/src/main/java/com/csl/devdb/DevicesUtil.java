package com.csl.devdb;

import java.util.List;

import com.ucsl.interfaces.IIDSLearnedRules;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;


public class DevicesUtil {

	/*
	 * leranedModel newlearnedmodel
	 * 
	 * idsmodel
	 * 
	 * devicesmodel
	 * 
	 * 
	 */

	/*
	 * verifier que toutes les IP de leranedmodel ds le modele devuces
	 * 
	 * 
	 */

	// idem pour les liens

//	static public void convert( IDSParams idsParams) {
//
//		String dir = idsParams.getIdsModelDir();
//		String fvar = idsParams.getVariablesFileName();
//		String fLearnRules = idsParams.getLearnedRulesFileName();
//
//		IDSVariables idsVariables = new IDSVariables(idsParams.getIdsMainProcessor());
//
//		idsVariables.readVariablesFromFile(dir, fvar, false);
//
//		IDSLearnedRules idsLearnedRules = new IDSLearnedRules(idsVariables);
//
//		idsLearnedRules.readFromFile(dir, fLearnRules);
//
//		System.out.println(idsLearnedRules);
//		System.out.println(idsLearnedRules.toJsonForTable());
//
//	}

	static public Json getLearnedModelTableAsJson(IIDSMainProcessor idsMainProcessor) { // idsParams) {

//		String dir = idsParams.getIdsModelDir();
//		String fvar = idsParams.getVariablesFileName();
//		String fLearnRules = idsParams.getLearnedRulesFileName();
//
//		IDSVariables idsVariables = new IDSVariables(idsParams.getIdsMainProcessor());
//
//		idsVariables.readVariablesFromFile(dir, fvar, false);

		IIDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);

		//idsLearnedRules.readFromFile(dir, fLearnRules);

		System.out.println(idsLearnedRules);
		System.out.println(idsLearnedRules.toJsonForTable());

		System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForTable()));

		// System.out.println("====RULES =====");
		// System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForRules()));

		// System.out.println("===============");
		return idsLearnedRules.toJsonForTable();

	}
	
	static public IIDSLearnedRules getLearnedModelFromFile(IIDSMainProcessor idsMainProcessor) { //IDSParams idsParams) {

//		String dir = idsParams.getIdsModelDir();
//		String fvar = idsParams.getVariablesFileName();
//		String fLearnRules = idsParams.getLearnedRulesFileName();
//
//		IDSVariables idsVariables = new IDSVariables(idsParams.getIdsMainProcessor());
//
//		idsVariables.readVariablesFromFile(dir, fvar, false);
//
//		IDSLearnedRules idsLearnedRules = new IDSLearnedRules(idsVariables);
//
//		
//		idsLearnedRules.readFromFile(dir, fLearnRules);
		IIDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);

		return idsLearnedRules;

		
	}

	static public Json addDevicesModelToAsJson(IIDSMainProcessor idsMainProcessor) {

//		String dir = idsParams.getIdsModelDir();
//		String fvar = idsParams.getVariablesFileName();
//		String fLearnRules = idsParams.getLearnedRulesFileName();
//
//		IDSVariables idsVariables = new IDSVariables(idsParams.getIdsMainProcessor());
//
//		idsVariables.readVariablesFromFile(dir, fvar, false);
//
//		IDSLearnedRules idsLearnedRules = new IDSLearnedRules(idsVariables);
//
//		idsLearnedRules.readFromFile(dir, fLearnRules);

		IIDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);

		System.out.println(idsLearnedRules);
		System.out.println(idsLearnedRules.toJsonForTable());

		System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForTable()));

		// System.out.println("====RULES =====");
		// System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForRules()));

		// System.out.println("===============");
		return idsLearnedRules.toJsonForTable();

	}

	static public Json getLearnedModelTableAsJsonDpi(IIDSMainProcessor idsMainProcessor) {

//		String dir = idsParams.getIdsModelDir();
//		String fvar = idsParams.getVariablesFileName();
//		String fLearnRules = idsParams.getLearnedRulesFileName();
//
//		IDSVariables idsVariables = new IDSVariables(idsParams.getIdsMainProcessor());
//
//		idsVariables.readVariablesFromFile(dir, fvar, false);
//
//		IDSLearnedRules idsLearnedRules = new IDSLearnedRules(idsVariables);
//
//		idsLearnedRules.readFromFile(dir, fLearnRules);
		
		IIDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);


		System.out.println(idsLearnedRules);
		System.out.println(idsLearnedRules.toJsonForTableDpi());

		return idsLearnedRules.toJsonForTableDpi();

	}

	

	

	private static Json copyProp(Json source, Json target, String name) {

		target.set(name, source.get(name));
		return target;
	}

	private static Json copyProps(Json source, Json target, List<String> list) {

		for (String name : list)
			target.set(name, source.get(name));
		return target;
	}

	private static Json copyProps(Json source, Json target, String[] list) {

		for (String name : list)
			target.set(name, source.get(name));
		return target;
	}

	// add the learned model to the devices model

	// add the leraned model to the devices database

	



}