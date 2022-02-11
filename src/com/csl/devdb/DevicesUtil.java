package com.csl.devdb;

import java.util.List;

import com.xcsl.ids.IDSMainProcessor;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;
import com.xcsl.learning.IDSLearnedRules;

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

	static public Json getLearnedModelTableAsJson(IDSMainProcessor idsMainProcessor) { // idsParams) {

//		String dir = idsParams.getIdsModelDir();
//		String fvar = idsParams.getVariablesFileName();
//		String fLearnRules = idsParams.getLearnedRulesFileName();
//
//		IDSVariables idsVariables = new IDSVariables(idsParams.getIdsMainProcessor());
//
//		idsVariables.readVariablesFromFile(dir, fvar, false);

		IDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);

		//idsLearnedRules.readFromFile(dir, fLearnRules);

		System.out.println(idsLearnedRules);
		System.out.println(idsLearnedRules.toJsonForTable());

		System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForTable()));

		// System.out.println("====RULES =====");
		// System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForRules()));

		// System.out.println("===============");
		return idsLearnedRules.toJsonForTable();

	}
	
	static public IDSLearnedRules getLearnedModelFromFile(IDSMainProcessor idsMainProcessor) { //IDSParams idsParams) {

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
		IDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);

		return idsLearnedRules;

		
	}

	static public Json addDevicesModelToAsJson(IDSMainProcessor idsMainProcessor) {

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

		IDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);

		System.out.println(idsLearnedRules);
		System.out.println(idsLearnedRules.toJsonForTable());

		System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForTable()));

		// System.out.println("====RULES =====");
		// System.out.println(JsonUtil.prettyPrint(idsLearnedRules.toJsonForRules()));

		// System.out.println("===============");
		return idsLearnedRules.toJsonForTable();

	}

	static public Json getLearnedModelTableAsJsonDpi(IDSMainProcessor idsMainProcessor) {

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
		
		IDSLearnedRules idsLearnedRules = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);


		System.out.println(idsLearnedRules);
		System.out.println(idsLearnedRules.toJsonForTableDpi());

		return idsLearnedRules.toJsonForTableDpi();

	}

	public static Json exportToJsonTable() {

		Json user = Json.object().set("name", "test");

		try {
			Json devices = DevicesDB.instance.exec(user, true, DevicesDB.GET_DEVICES, "", "", "", null, null);
			return devices;

		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Json.object().set("error", e.getMessage());
		}

	}

	public static Json exportToJson() {

		Json user = Json.object().set("name", "test");

		try {
			Json rows = Json.array();
			Json devices = DevicesDB.instance.exec(user, true, DevicesDB.GET_DEVICES, "", "", "", null, null);
			for (Json device : devices.asJsonList()) {
				Json jDevice = Json.object();
				jDevice.set("ip_src", device.get("ip"));
				jDevice.set("type", "node");

				// JsonUtil.copy(jSource,fieldName,jDest,fieldName,default;
				jDevice.set("country", JsonUtil.getStringFromJson(device, "props.country", ""));
				jDevice.set("name", JsonUtil.getStringFromJson(device, "props.name", ""));

				String mac = "";
				boolean first = true;
				Json extraMac = Json.array();
				for (Json m : device.get(DevicesDB.MACS).asJsonList()) {
					String s = m.asString();
					if (!first)
						extraMac.add(m);
					else {
						first = false;
						mac = s;
					}
				}
				jDevice.set("mac", mac);
				jDevice.set("extraMac", extraMac);
				jDevice.set("likelihood", JsonUtil.getStringFromJson(device, "props.likelihood", ""));
				jDevice.set("severity", JsonUtil.getStringFromJson(device, "props.severity", ""));
				jDevice.set("risk", JsonUtil.getStringFromJson(device, "props.risk", ""));

				rows.add(jDevice);

				Json dev_uuid = device.get(DevicesDB.UUID_TAG);

				Json links = DevicesDB.instance.exec(user, false, DevicesDB.GET_LINKS_FROM,
						device.get(DevicesDB.UUID_TAG).asString(), "", "", null, null);

				for (Json link : links.asJsonList()) {

					String uuidDst = link.get(DevicesDB.DST_UUID).asString();
					Json dst = DevicesDB.instance.exec(user, false, DevicesDB.GET_DEVICE, uuidDst, "", "", null, null);
					String ipDst = dst.get(DevicesDB.IP).asString();
					System.err.println(link.get(DevicesDB.UUID_TAG).asString());
					if ("541d0ca2-92a5-4de3-9f45-a4ab1ea7a0a3"
							.compareTo(link.get(DevicesDB.UUID_TAG).asString()) == 0) {
						System.err.println(link);
					}

					Json props = DevicesDB.instance.exec(user, true, DevicesDB.GET_LINK_PROPS_LIST_WITH_UUID,
							link.get(DevicesDB.UUID_TAG).asString(), "", "", null, null);

					for (Json prop : props.asJsonList()) {
						Json jLink = Json.object();

						jLink.set("ip_src", device.get("ip"));
						jLink.set("type", "link");
						rows.add(jLink);

						jLink.set("ip_dst", ipDst);
						jLink.set("port_src", JsonUtil.getIntFromJson(prop, "port_src", -1));
						jLink.set("port_dst", JsonUtil.getIntFromJson(prop, "port_dst", -1));

						jLink.set("protocol", JsonUtil.getStringFromJson(prop, "protocol", ""));
						jLink.set("app_protocol", JsonUtil.getStringFromJson(prop, "app_protocol", ""));

						jLink.set("likelihood", JsonUtil.getStringFromJson(prop, "likelihood", ""));
						jLink.set("severity", JsonUtil.getStringFromJson(prop, "severity", ""));
						jLink.set("risk", JsonUtil.getStringFromJson(prop, "risk", ""));

						jLink.set("size", JsonUtil.getLongFromJson(prop, "size", 0));
						jLink.set("packets", JsonUtil.getLongFromJson(prop, "packets", 0));

						jLink.set("maxKBpersec", JsonUtil.getLongFromJson(prop, "maxKBpersec", 0));
						jLink.set("maxpktpersec", JsonUtil.getLongFromJson(prop, "maxpktpersec", 0));

						jLink.set("first_time", JsonUtil.getLongFromJson(prop, "first_time", 0));
						jLink.set("last_time", JsonUtil.getLongFromJson(prop, "last_time", 0));
						System.out.println(jLink);

					}

				}

			}

			System.out.println(rows);
			DevicesDB.instance.dump();

			System.err.println(DevicesDB.instance.checkIntegrity());
			return rows;
		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Json.object().set("error", e.getMessage());
		}

	}

	public static Json updateDeviceMac(Json user, String ip, Json device, String mac) {

		if (device.get("macs") != null) {
			for (Json j : device.get("macs").asJsonList()) {
				if (mac.compareTo(j.asString()) == 0)
					return Json.object().set("op", "nop");
			}
		}
		try {
			DevicesDB.instance.exec(user, true, DevicesDB.SET_DEVICE_PROP, ip, "", "macs", Json.make(mac), null);
		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Json.object().set("op", DevicesDB.SET_DEVICE_PROP).set("ip", ip).set("mac", mac);
	}

	public static Json updateDeviceProp(Json user, String ip, Json device, String name, Json newValue, Json ops) {

		if (newValue == null)
			return Json.object().set("op", "nop");
		String s = newValue.asString();
		if (device.get("props") != null) {
			Json props = device.get("props");
			if (props.get(name) != null) {
				if (s.compareTo(props.get(name).asString()) == 0)
					return Json.object().set("op", "nop");
			}
		}

		try {
			DevicesDB.instance.exec(user, true, DevicesDB.SET_DEVICE_PROP, ip, "", "props." + name, Json.make(newValue),
					null);
		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Json op = Json.object().set("op", DevicesDB.SET_DEVICE_PROP).set("props." + name, s);
		ops.add(op);
		return op;
	}

	public static Json updateDevice(Json dev, Json user, Json ops) {
		try {

			String ip_src = dev.get("ip_src").asString();
			Json op = DevicesDB.instance.exec(user, true, DevicesDB.CREATE_IF_NOT_EXIST, ip_src, "", "", null, null);
			// if (op.get("op").asString().compareTo("nop")!=0) ops.add(op);

			Json device = DevicesDB.instance.exec(user, true, DevicesDB.GET_DEVICE, ip_src, "", "", null, null);

			String m = JsonUtil.getStringFromJson(dev, "mac", ""); // dev.get("mac").asString();

			if (!m.isEmpty())
				op = updateDeviceMac(user, ip_src, device, m);
			// if (op.get("op").asString().compareTo("nop")!=0) ops.add(op);

			if (dev.get("extraMac") != null) {
				for (Json js : dev.get("extraMac").asJsonList()) {
					op = updateDeviceMac(user, ip_src, device, js.asString());
					// if (op.get("op").asString().compareTo("nop")!=0) ops.add(op);

				}
			}

			if (dev.get("macs") != null) {
				for (Json js : dev.get("macs").asJsonList()) {
					op = updateDeviceMac(user, ip_src, device, js.asString());
					// if (op.get("op").asString().compareTo("nop")!=0) ops.add(op);

				}
			}
			/*
			 * j.set("ip_src", ip); j.set("name", name); j.set("mac", mac); if
			 * (extraMac!=null) { j.set("extraMac", extraMac); } j.set("likelihood",
			 * likelihood.getLevelAsString()); j.set("severity",
			 * severity.getLevelAsString()); j.set("country",country);
			 * 
			 * j.set("risk", getCriticity().getLevelAsString());
			 * 
			 */

			updateDeviceProp(user, ip_src, device, "name", dev.get("name"), ops);
			updateDeviceProp(user, ip_src, device, "severity", dev.get("severity"), ops);
			updateDeviceProp(user, ip_src, device, "likelihood", dev.get("likelihood"), ops);
			updateDeviceProp(user, ip_src, device, "risk", dev.get("risk"), ops);
			updateDeviceProp(user, ip_src, device, "country", dev.get("country"), ops);

		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Json.object().set("error", e.getMessage());
		}

		return ops;
	}

	public static Json updateLink(Json link, Json user, Json ops) {
		try {

			String ip_src = link.get("ip_src").asString();
			String ip_dst = link.get("ip_dst").asString();

			Json op = DevicesDB.instance.exec(user, true, DevicesDB.CREATE_IF_NOT_EXIST, ip_src, "", "", null, null);
//			if (op.get("op").asString().compareTo("nop")!=0) ops.add(op);

			op = DevicesDB.instance.exec(user, true, DevicesDB.CREATE_IF_NOT_EXIST, ip_dst, "", "", null, null);
//			if (op.get("op").asString().compareTo("nop")!=0) ops.add(op);

			Json selectProps = Json.object();
			Json replaceProps = Json.object();

			selectProps.set("port_src", link.get("port_src"));
			selectProps.set("port_dst", link.get("port_dst"));
			selectProps.set("protocol", link.get("protocol"));
			selectProps.set("app_protocol", link.get("app_protocol"));

			/*
			 * addRiskLevel2ToJson(jlink, entry2.getValue().riskLevel);
			 * 
			 * j.set("likelihood", r.getLikelyhoodLevel().getLevelAsString());
			 * j.set("severity", r.getSeverityLevel().getLevelAsString()); j.set("risk",
			 * r.getRiskLevel().getLevelAsString());
			 * 
			 * 
			 * jlink.set("protocol", sprot); jlink.set("app_protocol",
			 * entry2.getValue().getAppProtocol()); jlink.set("ip_src", ip_src);
			 * jlink.set("port_src", entry2.getValue().port_src);
			 * 
			 * jlink.set("ip_dst", entry2.getValue().ip_dst); jlink.set("port_dst",
			 * entry2.getValue().port_dst);
			 * 
			 * jlink.set("size", entry2.getValue().p_size); jlink.set("packets",
			 * entry2.getValue().n_packet);
			 * 
			 * jlink.set("maxKBpersec", entry2.getValue().max_p_size_by_period);
			 * jlink.set("maxpktpersec", entry2.getValue().max_n_packet_by_period);
			 * 
			 * 
			 * jlink.set("first_time", entry2.getValue().first_time); jlink.set("last_time",
			 * entry2.getValue().last_time);
			 */

			copyProps(link, replaceProps, new String[] { "risk", "likelihood", "severity", "size", "packets",
					"maxKBpersec", "maxpktpersec", "first_time", "last_time" });

			op = DevicesDB.instance.exec(user, true, DevicesDB.SELECT_AND_UPDATE_LINK_BETWEEN, ip_src, ip_dst, "",
					selectProps, replaceProps);
			// if (op.get("op").asString().compareTo("nop")!=0) ops.add(op);

		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Json.object().set("error", e.getMessage());
		}

		return ops;
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

	public static Json updateDevicesDBFromLearnedModel(IDSMainProcessor idsMainProcessor) {

		Json ops = Json.array();

		Json user = Json.object().set("name", "test");

//		String dir = idsParams.getIdsModelDir();
//		String fvar= idsParams.getVariablesFileName();
//		String fLearnRules= idsParams.getLearnedRulesFileName();
//
//		IDSVariables idsVariables = new IDSVariables();
//
//
//		idsVariables.readVariablesFromFile(dir, fvar,false);
//
//		IDSLearnedRules model = new IDSLearnedRules(idsVariables);
//		model.readFromFile(dir,fLearnRules);
//
//		DevicesDB.instance.updateLearnedModel(model);
//		
//		
//		model.writeToFile(dir,"gen_"+fLearnRules);
//		System.out.println(model);

		for (Json row : getLearnedModelTableAsJson(idsMainProcessor)) {
			System.out.println(row);

		}

		for (Json row : getLearnedModelTableAsJson(idsMainProcessor)) {
			System.out.println(row);
			if ("node".compareToIgnoreCase(row.get("type").asString()) == 0) {
				ops = updateDevice(row, user, ops);
			} else {
				ops = updateLink(row, user, ops);
			}
		}

		DevicesDB.instance.dump();

		System.out.println(ops);

		return ops;

	}

	// icijmfjmfjmf

	public static Json updateLearnedModelFromDevicesDB(IDSMainProcessor idsMainProcessor) {

		Json ops = Json.array();

		Json user = Json.object().set("name", "test");

//		String dir = idsParams.getIdsModelDir();
//		String fvar = idsParams.getVariablesFileName();
//		String fLearnRules = idsParams.getLearnedRulesFileName();

//		IDSVariables idsVariables = new IDSVariables(idsMainProcessor);
//
//		idsVariables.readVariablesFromFile(dir, fvar, false);
//
//		IDSLearnedRules model = new IDSLearnedRules(idsVariables);
//		// model.readFromFile(dir,fLearnRules);

		
		IDSLearnedRules model = idsMainProcessor.getLearnedModelFromFile(); // IDSLearnedRules(idsVariables);

		DevicesDB.instance.updateLearnedModel(model);

		idsMainProcessor.saveGeneratedLearnedModelToFile(model);
		
		//model.writeToFile(dir, "gen_" + fLearnRules);
		System.out.println(model);

		// essai
//		for (Json row:getLearnedModelTableAsJson(idsParams)) {
//			System.out.println(row);
//			if ("node".compareToIgnoreCase(row.get("type").asString())==0) {
//				ops=updateDevice(row,user,ops);
//			}
//			else {
//				ops=updateLink(row,user,ops);
//			}
//		}

		System.out.println(ops);

		return ops;

	}

}