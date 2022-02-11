package com.csl.devdb;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.csl.core.CSLContext;
import com.csl.util.GenericUtils;
import com.xcsl.ids.PacketInfo;
import com.xcsl.ids.risk.RiskLevel;
import com.xcsl.ids.risk.SeverityLevel;
import com.xcsl.json.Json;
import com.xcsl.json.JsonException;
import com.xcsl.json.JsonUtil;
import com.xcsl.learning.IDSLearnedRules;
import com.xcsl.util.TimeUtil;

public class DevicesDB2 {




	public static String FILE_NAME_DEVICES = "devices";

	static public DevicesDB2 instance = new DevicesDB2();
	static boolean initialized=false;

	// ===========================================

	public static final String IP = "ip";

	public static final String MACS = "macs";
	public static final String DEL_MAC = "del_mac";

	public static final String PROPS = "props";
	public static final String MPROPS = "mprops";

	public static final String DEVICES_LIST = "devices";

	public static final String LINKS_LIST = "links";
	public static final String NETWORK_VUES_LIST = "networkVues";

	public static final String LINKS_TO = "links_to";

	public static final String LINKS_FROM = "links_from";

	public static final String UUID_TAG = "uuid";

	public static final String _ID = "_id";

	public static final String DST_UUID = "dst_uuid";

	public static final String SRC_UUID = "src_uuid";

	public static final String PROPS_LIST = "props_list";

	public static final String NOP = "nop";

	public HashMap<String, String> dictIPtoUUID = new HashMap<String, String>();
	public HashMap<String, Json> dictUUIDToDevice = new HashMap<String, Json>();

	public HashMap<String, Json> dictUUIDToLink = new HashMap<String, Json>();

	Json devices = Json.array();
	Json links = Json.array();
	Json networkVues = Json.object();

	private boolean updated;


	private Json getDeviceDefaultProps() {

		Json p= Json.object();

		p.set("macs", Json.array());
		p.set("tap_networks", Json.array());

		return p;

	}

	private DevicesDB2() {
		// TODO Auto-generated constructor stub

		//loadFiles();
	}

	/*
	 * 
	 * 
	 * device: { uuid : ip : macs : props : object with os, softwares ... links : []
	 * }
	 * 
	 * 
	 *
	 * links: { src_uuid dst_uuid props_list: [{ _id: ; // id of the props set
	 *
	 * port_src port_dst protocol app_protocol }] }
	 */

	/*
	 * op : add_device del_device set_device_prop path : props props.subpath ip macs
	 * macs.i
	 * 
	 * list_devices
	 * 
	 * 
	 */

	/*
	 * devices setConfidenceLevel(value, source) addMac(value, source)
	 * 
	 * histo: propname mode : add or set [source, source ...] source: {type:'manu",
	 * 'ra', 'learn', 'al', 'as'] (manual, riks analysis, learning, alert, active
	 * scan
	 * 
	 * operation setManagedProp(json, name, value), unSetManaged(json, name, value,
	 * default) getManagedValue(json,name)
	 * 
	 * links setAnomalyLevel(value, source)
	 * 
	 */

	public static String ADD_DEVICE = "ADD_DEVICE";
	static String DEL_DEVICE = "DEL_DEVICE";
	static String GET_DEVICE = "GET_DEVICE";

	static String SET_DEVICE_PROP = "SET_DEVICE_PROP";
	static String GET_DEVICE_PROP = "GET_DEVICE_PROP";

	static String SET_DEVICE_MPROP = "SET_DEVICE_MPROP";
	static String GET_DEVICE_MPROP = "GET_DEVICE_MPROP";


	static String ADD_DEVICE_MPROP = "ADD_DEVICE_MPROP";
	static String DEL_DEVICE_MPROP = "DEL_DEVICE_MPROP";
	static String UNDO_LAST_DEVICE_MPROP = "UNDO_LAST_DEVICE_MPROP";
	static String UNDO_ALL_DEVICE_MPROP = "UNDO_ALL_DEVICE_MPROP";


	static String GET_DEVICES = "LST_DEVICES";
	static String UPDATE_DEVICES = "UPDATE_DEVICES";

	static String GET_NETWORK_VUES = "GET_NETWORK_VUES";
	static String SET_NETWORK_VUES = "SET_NETWORK_VUES";

	static String ADD_LINK = "ADD_LINK";
	static String DEL_LINK = "DEL_LINK";
	static String GET_LINK = "GET_LINK";
	static String GET_LINK_BETWEEN = "GET_LINK_BETWEEN";
	static String SELECT_AND_UPDATE_LINK_BETWEEN = "SELECT_AND_UPDATE_LINK_BETWEEN";

	static String GET_LINKS_FROM = "GET_LINKS_FROM";
	static String GET_LINKS_TO = "GET_LINKS_TO";

	static String GET_LINK_PROPS_LIST = "GET_LINK_PROPS_LIST";
	static String GET_LINK_PROPS_LIST_WITH_UUID = "GET_LINK_PROPS_LIST_WITH_UUID";

	static String ADD_LINK_PROPS = "ADD_LINK_PROPS";
	static String DEL_LINK_PROPS = "DEL_LINK_PROPS";
	static String SET_LINK_PROPS = "SET_LINK_PROPS";
	// path starts with # --> id
	// path starts with number. --> path to the props_list array



	static String GET_LINK_PROPS = "GET_LINK_PROPS";

	static String SET_LINK_PROP = "SET_LINK_PROP";
	static String GET_LINK_PROP = "GET_LINK_PROP";


	static String SET_LINK_MPROP = "SET_LINK_MPROP";
	static String GET_LINK_MPROP = "GET_LINK_MPROP";

	static String ADD_LINK_MPROP = "ADD_LINK_MPROP";
	static String DEL_LINK_MPROP = "DEL_LINK_MPROP";
	static String UNDO_LAST_LINK_MPROP = "UNDO_LAST_LINK_MPROP";
	static String UNDO_ALL_LINK_MPROP = "UNDO_ALL_LINK_MPROP";


	static String SELECT_LINK_ON_PROP = "GET_LINK_PROP";

	static String CLEAR = "CLEAR";

	static String SET_DEBUG = "SET_DEBUG";

	public boolean debug = true;

	//	ajouter la
	//	liste des
	//	pros managéees
	//
	//	mofier setprops, pour
	//	mettre uen
	//	par une
	//
	//	gretprops enlever histo

	/*
	 * user : Json describing the user (for logging of actions) modeip the
	 * identifier is ip if true, uuid if false op iuuidOrIP path newParams oldP
	 */
	public Json exec(Json user, boolean modeIp, String op, String uuidOrIp, String id2, String path, Json value,Json value2
			) throws DevicesDBException {

		if (!initialized) {
			loadFiles();
			initialized=true;
		}

		Json result = Json.object();
		if (debug)
			System.out.println("EXEC_DB_DEVICES_OP:" + op);

		if (ADD_DEVICE.compareToIgnoreCase(op) == 0) {
			result = createDevice(modeIp, uuidOrIp, value);
			setUpdated(true);
		} else if (DEL_DEVICE.compareToIgnoreCase(op) == 0) {
			delDevice(modeIp, uuidOrIp);
			setUpdated(true);
		} else if (GET_DEVICE.compareToIgnoreCase(op) == 0) {
			result = getDevice(modeIp, uuidOrIp);
			setUpdated(true);
		} else if (SET_DEVICE_PROP.compareToIgnoreCase(op) == 0) {

			result = updateDevice(modeIp, uuidOrIp, path, value);
			setUpdated(true);

		}
		else if (GET_DEVICE_PROP.compareToIgnoreCase(op) == 0) {
			return getDeviceProp(modeIp, uuidOrIp, path);

		} else if (GET_DEVICES.compareToIgnoreCase(op) == 0) {
			return getDevicesList();
			//} else if (GET_DEVICES.compareToIgnoreCase(op) == 0) {
			//	return devices;
		} else if (UPDATE_DEVICES.compareToIgnoreCase(op) == 0) {
			result = createDeviceIfNotExist(uuidOrIp); // only with ip
			setUpdated(true);

		} else if (SET_DEVICE_MPROP.compareToIgnoreCase(op) == 0) {
			result = devicesManagedProp(user,"set",modeIp, uuidOrIp, path, value);
			setUpdated(true);

		} else if (ADD_DEVICE_MPROP.compareToIgnoreCase(op) == 0) {
			result = devicesManagedProp(user,"add",modeIp, uuidOrIp, path, value);
			setUpdated(true);

		} else if (DEL_DEVICE_MPROP.compareToIgnoreCase(op) == 0) {
			result = devicesManagedProp(user,"del",modeIp, uuidOrIp, path, value);
			setUpdated(true);

		} else if (UNDO_LAST_DEVICE_MPROP.compareToIgnoreCase(op) == 0) {
			result = devicesManagedProp(user,"undo_last",modeIp, uuidOrIp, path, value);
			setUpdated(true);

		} else if (UNDO_ALL_DEVICE_MPROP.compareToIgnoreCase(op) == 0) {
			result = devicesManagedProp(user,"undo_all",modeIp, uuidOrIp, path, value);
			setUpdated(true);

		} else if (GET_DEVICE_MPROP.compareToIgnoreCase(op) == 0) {
			result = devicesManagedProp(user,"get",modeIp, uuidOrIp, path, value);

		} 
		// LINK
		else if (ADD_LINK.compareToIgnoreCase(op) == 0) {
			result = addLink(modeIp, uuidOrIp, id2, value);
			setUpdated(true);
		} else if (DEL_LINK.compareToIgnoreCase(op) == 0) {
			result = delLink(modeIp, uuidOrIp, id2);
			setUpdated(true);
		} else if (GET_LINK_BETWEEN.compareToIgnoreCase(op) == 0) {
			result = getLink(modeIp, uuidOrIp, id2);

		} else if (SELECT_AND_UPDATE_LINK_BETWEEN.compareToIgnoreCase(op) == 0) {
			result = selectAndUpdateLink(modeIp, uuidOrIp, id2, value, value2);
			if (result.get("op").asString().compareTo("nop") != 0)
				setUpdated(true);
		} else if (GET_LINKS_FROM.compareToIgnoreCase(op) == 0) {
			result = getLinksFrom(modeIp, uuidOrIp);

		} else if (GET_LINKS_TO.compareToIgnoreCase(op) == 0) {
			result = getLinksTo(modeIp, uuidOrIp);

		} else if (GET_LINK_PROPS_LIST.compareToIgnoreCase(op) == 0) {
			result = getLinkPropsList(modeIp, uuidOrIp, id2);

		} else if (GET_LINK_PROPS_LIST_WITH_UUID.compareToIgnoreCase(op) == 0) {
			result = getLinkPropsListWithUUID(modeIp, uuidOrIp, id2);

		} else if (GET_LINK_PROPS.compareToIgnoreCase(op) == 0) {
			result = getLinkProps(modeIp, uuidOrIp, id2, path);
		} else if (SET_LINK_PROPS.compareToIgnoreCase(op) == 0) {
			setUpdated(true);
			return setLinkProps(modeIp, uuidOrIp, id2, path, value);
		} else if (ADD_LINK_PROPS.compareToIgnoreCase(op) == 0) {
			setUpdated(true);
			result = addLinkProps(modeIp, uuidOrIp, id2, value);
		} else if (DEL_LINK_PROPS.compareToIgnoreCase(op) == 0) {
			setUpdated(true);
			return delLinkProps(modeIp, uuidOrIp, id2, path);

		} else if (SET_LINK_MPROP.compareToIgnoreCase(op) == 0) {
			result = linksManagedProp(user,"set",modeIp, uuidOrIp,id2, path, value);
			setUpdated(true);

		} else if (ADD_LINK_MPROP.compareToIgnoreCase(op) == 0) {
			result = linksManagedProp(user,"add",modeIp, uuidOrIp,id2, path, value);
			setUpdated(true);

		} else if (DEL_LINK_MPROP.compareToIgnoreCase(op) == 0) {
			result = linksManagedProp(user,"del",modeIp, uuidOrIp,id2, path, value);
			setUpdated(true);

		} else if (UNDO_LAST_LINK_MPROP.compareToIgnoreCase(op) == 0) {
			result = linksManagedProp(user,"undo_last",modeIp, uuidOrIp,id2, path, value);
			setUpdated(true);

		} else if (UNDO_ALL_LINK_MPROP.compareToIgnoreCase(op) == 0) {
			result = linksManagedProp(user,"undo_all",modeIp, uuidOrIp,id2, path, value);
			setUpdated(true);

		} else if (GET_LINK_MPROP.compareToIgnoreCase(op) == 0) {
			result = linksManagedProp(user,"get",modeIp, uuidOrIp, id2,path, value);

		} 
		else if (SET_DEBUG.compareTo(op) == 0) {
			if (uuidOrIp.compareToIgnoreCase("on") == 0)
				debug = true;
			else
				debug = false;
		} else if (CLEAR.compareTo(op) == 0) {
			clear();
		}

		else if (GET_NETWORK_VUES.compareToIgnoreCase(op) == 0) {
			return networkVues;
		} else if (SET_NETWORK_VUES.compareToIgnoreCase(op) == 0) {
			networkVues = value;
			setUpdated(true);
		}

		else {
			result.set("error", "Invalid op:" + op);
		}

		if (updated)
			saveFiles();
		List<String> errors = checkIntegrity();
		if (errors.size() != 0) {
			for (String e : errors) {
				System.err.println(e);
			}
		}

		return result;
	}



	public Json getDevicesList() {

		Json list= Json.array();
		for (Json j:devices.asJsonList()) {
			Json d=Json.object();
			d.set("ip",  JsonUtil.getStringFromJson(j, "ip", ""));
			d.set("uuid",  JsonUtil.getStringFromJson(j, "uuid", ""));

			//ajouter copie de props ici

			Json props;
			if (j.has("props"))
				props=j.get("props");
			else
				props=Json.object();

			Json props2=getDeviceDefaultProps();

			for (String key : props.asJsonMap().keySet()) {
				if (key.compareTo("__histo")!=0) {
					props2.set(key,props.get(key)) ;
				}

			}
			d.set("props",props2);



			list.add(d);
		}
		return list;
	}

	public void clear() {

		dictIPtoUUID = new HashMap<String, String>();
		dictUUIDToDevice = new HashMap<String, Json>();

		dictUUIDToLink = new HashMap<String, Json>();

		devices = Json.array();
		links = Json.array();

	}

	public Json createDeviceIfNotExist(String ip) throws DevicesDBException {

		if (dictIPtoUUID.get(ip) != null)
			return Json.object().set("op", NOP);
		UUID uuidx = UUID.randomUUID();
		String uuid = uuidx.toString();
		Json device = Json.object();

		device.set(UUID_TAG, uuid);
		device.set(IP, ip);

		device.set(MACS, Json.array());

		device.set(PROPS, Json.object());
		device.set(LINKS_FROM, Json.array());
		device.set(LINKS_TO, Json.array());

		int n = devices.asJsonList().size();
		devices.add(device);
		addToDict(device.get(IP).asString(), device.get(UUID_TAG).asString(), device);

		return Json.object().set("op", UPDATE_DEVICES);

	}

	public Json createDevice(boolean modeIp, String uuidOrIp, Json params) throws DevicesDBException {

		String ip = "";
		String uuid = "";
		if (modeIp)
			ip = uuidOrIp;
		else
			uuid = uuidOrIp;
		if (params == null)
			params = Json.object();

		if (ip.isEmpty()) {
			if (!params.has(IP)) {
				throw new DevicesDBException("Missing ip to create device");
			} else
				ip = params.get(IP).asString();
		}

		if (dictIPtoUUID.get(ip) != null) {
			throw new DevicesDBException("Device with same ip already in database (ip=" + params.get(IP) + ")");
		}

		if (uuid == null)
			uuid = "";
		if (uuid.isEmpty()) {
			UUID uuidx = UUID.randomUUID();
			uuid = uuidx.toString();
		}

		updated = true;
		Json device = Json.object();
		device.set(UUID_TAG, uuid);

		device.set(IP, ip);
		if (params.has(MACS))
			device.set(MACS, params.get(MACS));
		else {
			device.set(MACS, Json.array());
		}
		if (params.has(PROPS))
			device.set(PROPS, params.get(PROPS));
		else {
			device.set(PROPS, Json.object());
		}
		// if (params.has("links"))
		// device.set("links", params.get("links"));
		// else {
		// device.set("links", Json.array());
		// }
		device.set(LINKS_FROM, Json.array());
		device.set(LINKS_TO, Json.array());

		int n = devices.asJsonList().size();
		devices.add(device);
		addToDict(device.get(IP).asString(), device.get(UUID_TAG).asString(), device);

		return device;

	}

	private void addToDict(String ip, String uuid, Json device) {
		System.out.println("ADD " + ip + " ==>" + uuid);
		dictIPtoUUID.put(ip, uuid);
		dictUUIDToDevice.put(uuid, device);

	}

	/*
	 * ip macs props. + path
	 * 
	 */
	public Json getDeviceProp(boolean modeIp, String uuidOrIp, String path) throws DevicesDBException {

		String uuid = "";
		if (modeIp)
			uuid = dictIPtoUUID.get(uuidOrIp);
		else
			uuid = uuidOrIp;
		if (uuid == null)
			throw new DevicesDBException("Invalid ip in update device  " + uuidOrIp);
		;

		Json device = dictUUIDToDevice.get(uuid);
		if (device == null)
			throw new DevicesDBException("Invalid uuid in update device " + uuid);

		if (path.compareTo(IP) == 0) {
			return device.get(IP);
		}
		if (path.compareTo(MACS) == 0) {
			return device.get(MACS);
		}

		if (!path.startsWith(PROPS))
			new DevicesDBException("path must be macs or starts with props:" + path);

		try {
			return JsonUtil.getElement(device, path);
		} catch (JsonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Json.object();
	}

	private Json updateDevice(boolean modeIp, String uuidOrIp, String path, Json value) throws DevicesDBException {

		String uuid = "";
		if (modeIp)
			uuid = dictIPtoUUID.get(uuidOrIp);
		else
			uuid = uuidOrIp;
		if (uuid == null)
			throw new DevicesDBException("Invalid ip in update device  " + uuidOrIp);
		;

		Json device = dictUUIDToDevice.get(uuid);
		if (device == null)
			throw new DevicesDBException("Invalid uuid in update device " + uuid);

		if (path.compareTo(IP) == 0) {
			// throw new DevicesDBException("ip modification not implemented");
			if (!value.isString())
				throw new DevicesDBException("IP must be a string");
			device.set(IP, value);
		}
		if (path.compareTo(MACS) == 0) {
			if (!value.isString())
				throw new DevicesDBException("Can only add string to macs");
			for (Json jj : device.get(MACS).asJsonList()) {
				if (jj.asString().compareTo(value.asString()) == 0)
					throw new DevicesDBException("mac already added " + value.asString());
			}
			device.get(MACS).add(value);
			return device;
		}
		if (path.compareTo(DEL_MAC) == 0) {
			if (!value.isString())
				throw new DevicesDBException("Can only add string to macs");

			List<String> ls = new ArrayList<String>();
			for (Json jj : device.get(MACS).asJsonList()) {
				if (jj.asString().compareTo(value.asString()) != 0)
					ls.add(jj.asString());
			}
			device.set(MACS, Json.array());
			for (String s : ls) {
				device.get(MACS).add(value);
			}

			return device;
		}

		// MANAGED PROPS
		if (path.startsWith(MPROPS)) {

		}

		else if (!path.startsWith(PROPS))
			new DevicesDBException("path must be macs or starts with props:" + path);

		try {
			JsonUtil.setElement(device, path, value, true);
		} catch (JsonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return device;
	}


	// OPM
	private Json devicesManagedProp(Json user,String scode, boolean modeIp, String uuidOrIp, String path, Json value) throws DevicesDBException {
		String uuid = "";
		if (modeIp)
			uuid = dictIPtoUUID.get(uuidOrIp);
		else
			uuid = uuidOrIp;
		if (uuid == null)
			throw new DevicesDBException("Invalid ip in update device  " + uuidOrIp);
		;

		Json device = dictUUIDToDevice.get(uuid);
		if (device == null)
			throw new DevicesDBException("Invalid uuid in update device " + uuid);


		if (path.startsWith("props.")) path=path.substring("props.".length());

		int op=opCodeForManagedProp(scode);
		if (op<0) {
			throw new DevicesDBException("Invalid op code "+scode);
		}
		String type=JsonUtil.getStringFromJson(user, "type","undef");
		String id=JsonUtil.getStringFromJson(user, "id","");
		Json source =Json.object().set("type",type).set("id", id);

		Json props=device.get(PROPS);
		if (props==null) {device.set(PROPS, Json.object());props=device.get(PROPS);}


		// DEL

		// GET
		// name 			return list or elt 
		// name[i]			return elt of list
		// name.len		return size


		if ((op>=1)&&(op<=3)) {
			if ((op==2)||(op==3)) {
				if (!value.isString())
					return Json.object().set("result", "").set("error", "value must be a string for props list");
			}
			opOnManagedProp(op, props, path, value,source );
		}
		else if (op==4) {
			undoLastOpOnManagedProp(props, path, source);
		}
		else if (op==5) {
			undoAllOpOnManagedProp(props, path, source);
		}
		else if (op==6) {
			if (path.endsWith(".size")) {
				path=path.substring(0,path.length()-".size".length());
				Json p= props.get(path);
				if (p==null) return Json.object().set("result", "").set("error", "invalid prop name");
				int n=1;
				if (p.isArray()) n= p.asJsonList().size();
				return Json.object().set("size", n);
			}
			if (path.endsWith("]")) {
				int p=path.lastIndexOf("[");
				String name=path.substring(0,p);
				String sindex=path.substring(p+1,path.length()-1);

				int index=1;
				Json j= props.get(name);
				if (j==null) return Json.object().set("result", "").set("error", "invalid prop name");

				index= GenericUtils.str2int(sindex, -1);
				if (j.isArray()) {
					if((index>=0)&&(index<j.asJsonList().size())) {
						return j.asJsonList().get(index);
					}
					else
						return Json.object().set("result", "").set("error", "invalid prop name");
				}
				else {
					return j;
				}

			}
			Json p= props.get(path);
			if (p==null) return Json.object().set("result", "").set("error", "invalid prop name");
			return p;
		}

		return Json.object("result", "ok");
	}


	private void delDevice(boolean modeIp, String uuidOrIp) throws DevicesDBException {
		// int n=0;
		// List<Json> list = devices.asJsonList();
		//
		// int index=-1;
		// for (int i=0;i<list.size();i++) {
		// Json x=list.get(i).get("uuid");
		// if (x!=null) {
		// if (list.get(i).get("uuid").asString().compareTo(uuid)==0) {
		// index=i;
		// break;
		// }
		// }
		// }

		String uuid = "";
		if (modeIp)
			uuid = dictIPtoUUID.get(uuidOrIp);
		else
			uuid = uuidOrIp;
		if (uuid == null)
			throw new DevicesDBException("Invalid ip in delete device  " + uuidOrIp);
		;

		if (debug)
			System.out.println("DEL DEVICE:" + uuid + " " + getIp(uuid));

		Json device = dictUUIDToDevice.get(uuid);
		if (!device.has(IP))
			throw new DevicesDBException("Invalid ip for the device " + uuid);

		if (device == null)
			throw new DevicesDBException("Cannot find device for uuid " + uuid);

		delAllLinks(false, uuid);
		devices.asJsonList().remove(device);

		String ip = device.get(IP).asString();

		dictIPtoUUID.remove(ip);
		dictUUIDToDevice.remove(uuid);

	}

	public Json getListOfUUID() {

		List<String> l = new ArrayList<String>(dictUUIDToDevice.keySet());
		Json j = Json.array();
		for (String s : l)
			j.add(s);
		return j;
	}

	// array of Json with full devices
	public Json getListOfDevices() {

		return devices;
	}

	private Json findLink(Json dev1, Json dev2) {

		Json links1 = dev1.get(LINKS_FROM);
		if (links1 == null)
			return null;
		String uuid1 = dev1.get(UUID_TAG).asString();

		Json links2 = dev2.get(LINKS_TO);
		if (links == null)
			return null;
		String uuid2 = dev2.get(UUID_TAG).asString();

		for (Json luuid : links1.asJsonList()) {
			Json l = dictUUIDToLink.get(luuid.asString());
			// String src= l.get(SRC_UUID).asString();
			String dst = l.get(DST_UUID).asString();
			// if ( ((uuid1.compareTo(src)==0)&&(uuid2.compareTo(dst)==0))||
			// ((uuid1.compareTo(dst)==0)&&(uuid2.compareTo(src)==0))) return l;
			if (uuid2.compareTo(dst) == 0)
				return l;
		}

		// // this part is not useful (shiuld have been found above)
		// for (Json luuid:links2.asJsonList()) {
		// Json l=dictUUIDToLink.get(luuid.asString());
		// String src= l.get(SRC_UUID).asString();
		// String dst= l.get(DST_UUID).asString();
		// if ( ((uuid1.compareTo(src)==0)&&(uuid2.compareTo(dst)==0))||
		// ((uuid1.compareTo(dst)==0)&&(uuid2.compareTo(src)==0))) return l;
		// }
		return null;

	}

	private boolean findFromLink(Json dev1, String link_uuid) {

		Json links1 = dev1.get(LINKS_FROM);
		if (links1 == null)
			return false;
		for (Json luuid : links1.asJsonList()) {
			if (luuid.toString().compareTo(link_uuid) == 0)
				return true;
		}
		return false;
	}

	private boolean findToLink(Json dev1, String link_uuid) {

		Json links1 = dev1.get(LINKS_TO);
		if (links1 == null)
			return false;
		for (Json luuid : links1.asJsonList()) {
			if (luuid.toString().compareTo(link_uuid) == 0)
				return true;
		}
		return false;
	}

	public Json getDevice(boolean modeIp, String uuidOrIpSrc) throws DevicesDBException {

		String uuidSrc = "";
		if (modeIp)
			uuidSrc = dictIPtoUUID.get(uuidOrIpSrc);
		else
			uuidSrc = uuidOrIpSrc;
		if (uuidSrc == null)
			throw new DevicesDBException("Invalid ip src in find link " + uuidOrIpSrc);

		Json deviceSrc = dictUUIDToDevice.get(uuidSrc);
		if (deviceSrc == null)
			throw new DevicesDBException("Invalid uuid src in find link " + uuidSrc);

		return deviceSrc;
	}

	public Json findLink(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst) throws DevicesDBException {

		String uuidSrc = "";
		if (modeIp)
			uuidSrc = dictIPtoUUID.get(uuidOrIpSrc);
		else
			uuidSrc = uuidOrIpSrc;
		if (uuidSrc == null)
			throw new DevicesDBException("Invalid ip src in find link " + uuidOrIpSrc);

		String uuidDst = "";
		if (modeIp)
			uuidDst = dictIPtoUUID.get(uuidOrIpDst);
		else
			uuidDst = uuidOrIpDst;
		if (uuidDst == null)
			throw new DevicesDBException("Invalid ip dst in find link " + uuidOrIpDst);

		Json deviceSrc = dictUUIDToDevice.get(uuidSrc);
		if (deviceSrc == null)
			throw new DevicesDBException("Invalid uuid src in find link " + uuidSrc);

		Json deviceDst = dictUUIDToDevice.get(uuidDst);
		if (deviceDst == null)
			throw new DevicesDBException("Invalid uuid dst in find link " + uuidDst);

		Json linkSrc = deviceSrc.get(LINKS_FROM);
		if (linkSrc == null) {
			linkSrc = Json.array();
			deviceSrc.set(LINKS_FROM, linkSrc);
		}

		Json linkDst = deviceDst.get(LINKS_TO);
		if (linkDst == null) {
			linkDst = Json.array();
			deviceDst.set(LINKS_TO, linkDst);
		}

		Json l = findLink(deviceSrc, deviceDst);
		return l;
	}

	public Json getLink(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst) throws DevicesDBException {

		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);
		if (l == null) {
			Json j = Json.object();
			j.set("result", "NO_LINK");
			j.set("uuid", "#null");
			j.set("error", "NO_LINK");
			return j;
		}
		return l;

	}

	public static Json concat(Json p1, Json p2) {
		// TODO Auto-generated method stub
		Json result= Json.object();

		for (String key:p1.asJsonMap().keySet()) {
			result.set(key,  p1.get(key));
		}


		for (String key:p2.asJsonMap().keySet()) {
			result.set(key,  p2.get(key));
		}

		return result;
	}


	public Json selectAndUpdateLink(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst, Json selectProps,
			Json replaceProps) throws DevicesDBException {

		modeIp = true;
		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);

		if (l == null) {
			replaceProps = JsonUtil.concat(replaceProps, selectProps);
			addLink(modeIp, uuidOrIpSrc, uuidOrIpDst, replaceProps);
			Json op = Json.object().set("op", ADD_LINK).set("ip_src", uuidOrIpSrc).set("ip_dst", uuidOrIpDst)
					.set("props", replaceProps);
			return op;
		}

		// iterate props_list
		for (Json j : l.get(PROPS_LIST).asJsonList()) {
			boolean found = testValues(j, selectProps);
			if (found) {
				Json result = updateLink(l, j, replaceProps);
				if (result.asJsonMap().size() > 0) {
					Json.object().set("op", SELECT_AND_UPDATE_LINK_BETWEEN).set("ip_src", uuidOrIpSrc)
					.set("ip_dst", uuidOrIpDst).set("props", replaceProps).set("selectProps", selectProps);
				} else
					return Json.object().set("op", NOP);
			}
		}

		replaceProps = JsonUtil.concat(replaceProps, selectProps);  
		addLinkProps(modeIp, uuidOrIpSrc, uuidOrIpDst, replaceProps);

		Json op = Json.object().set("op", ADD_LINK_PROPS).set("ip_src", uuidOrIpSrc).set("ip_dst", uuidOrIpDst)
				.set("props", replaceProps);
		return op;

	}

	private Json updateLink(Json link, Json props, Json replaceProps) {



		Json modifiedProps = Json.object();

		for (String key : replaceProps.asJsonMap().keySet()) {
			if (props.get(key) != null) {
				if (props.get(key).asString().trim().compareTo(replaceProps.get(key).asString().trim()) != 0) {
					props.set(key, replaceProps.get(key));
					modifiedProps.set(key, replaceProps.get(key));
				}
			} else {
				props.set(key, replaceProps.get(key));
				modifiedProps.set(key, replaceProps.get(key));
			}
		}

		return modifiedProps;
	}

	private boolean testValues(Json j, Json selectProps) {
		for (String key : selectProps.asJsonMap().keySet()) {
			if (j.get(key) != null) {
				if (j.get(key).asString().trim().compareTo(selectProps.get(key).asString().trim()) != 0)
					return false;
			} else
				return false;
		}
		return true;
	}

	public Json getLinksTo(boolean modeIp, String uuidOrIpDst) throws DevicesDBException {

		String uuidDst = "";
		if (modeIp)
			uuidDst = dictIPtoUUID.get(uuidOrIpDst);
		else
			uuidDst = uuidOrIpDst;
		if (uuidDst == null)
			throw new DevicesDBException("Invalid ip dst in find link " + uuidOrIpDst);

		Json deviceDst = dictUUIDToDevice.get(uuidDst);
		if (deviceDst == null)
			throw new DevicesDBException("Invalid uuid dst in find link " + uuidDst);

		Json linkDst = deviceDst.get(LINKS_TO);
		if (linkDst == null) {
			linkDst = Json.array();
			deviceDst.set(LINKS_TO, linkDst);
		}

		Json result = Json.array();
		for (Json luuid : linkDst.asJsonList()) {
			Json l = dictUUIDToLink.get(luuid.asString());
			result.add(l);
		}
		return result;

	}

	public Json getLinksFrom(boolean modeIp, String uuidOrIpSrc) throws DevicesDBException {

		String uuidSrc = "";
		if (modeIp)
			uuidSrc = dictIPtoUUID.get(uuidOrIpSrc);
		else
			uuidSrc = uuidOrIpSrc;
		if (uuidSrc == null)
			throw new DevicesDBException("Invalid ip src in find link " + uuidOrIpSrc);

		Json deviceSrc = dictUUIDToDevice.get(uuidSrc);
		if (deviceSrc == null)
			throw new DevicesDBException("Invalid uuid src in find link " + uuidSrc);

		Json linkSrc = deviceSrc.get(LINKS_FROM);
		if (linkSrc == null) {
			linkSrc = Json.array();
			deviceSrc.set(LINKS_FROM, linkSrc);
		}

		Json result = Json.array();
		for (Json luuid : linkSrc.asJsonList()) {
			Json l = dictUUIDToLink.get(luuid.asString());
			// System.out.println(l);
			result.add(l);
		}

		return result;

	}

	public Json getLinkPropsList(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst) throws DevicesDBException {

		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);
		if (l == null)
			throw new DevicesDBException("Cannot find link for prop list between" + uuidOrIpSrc + "  " + uuidOrIpDst);

		return l.get(PROPS_LIST);

	}

	public Json getLinkPropsListWithUUID(boolean bnotused, String uuidlink, String notused) throws DevicesDBException {

		Json l = dictUUIDToLink.get(uuidlink);
		if (l == null)
			throw new DevicesDBException("Cannot find link with uuid" + uuidlink);

		return l.get(PROPS_LIST);

	}

	public Json getLinkProps(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst, String id)
			throws DevicesDBException {

		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);
		if (l == null)
			throw new DevicesDBException("Cannot find link to delete between" + uuidOrIpSrc + "  " + uuidOrIpDst);

		if (id.startsWith("#")) {
			for (Json j : l.get(PROPS_LIST).asJsonList()) {
				Json key = j.get(_ID);
				if (key != null) {
					if (key.isString()) {
						if (id.compareTo(key.asString()) == 0)
							return j;
					}

				}
			}
		} else {

			// System.out.println(l.get(PROPS_LIST));
			try {

				return JsonUtil.getElement(l.get(PROPS_LIST), id);

			} catch (JsonException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;

	}

	private Json getLinkPropsForId(String id, Json link) {
		if (id.startsWith("#")) {
			for (Json j : link.get(PROPS_LIST).asJsonList()) {
				Json key = j.get(_ID);
				if (key != null) {
					if (key.isString()) {
						if (id.compareTo(key.asString()) == 0)
							return j;
					}
				}
			}
		} else {
			try {
				return JsonUtil.getElement(link.get(PROPS_LIST), id);
			} catch (JsonException e) {
				e.printStackTrace();
			}
		}
		return null;
	}


	private Json linksManagedProp(Json user,String scode, boolean modeIp, String uuidOrIpSrc,String uuidOrIpDst, String path, Json value) throws DevicesDBException {



		// path : id. nmae
		int idx= path.indexOf('.');
		if (idx<0)
			throw new DevicesDBException("Invalid path to acces link props :"+path);
		String id = path.substring(0, idx);
		path= path.substring(idx+1, path.length());

		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);
		if (l == null)
			throw new DevicesDBException("Cannot find link to delete between" + uuidOrIpSrc + "  " + uuidOrIpDst);

		Json props= getLinkPropsForId(id, l);
		if (props==null)
			throw new DevicesDBException("Cannot find link props with id " +path);

		int op=opCodeForManagedProp(scode);
		if (op<0) {
			throw new DevicesDBException("Invalid op code "+scode);
		}
		String type=JsonUtil.getStringFromJson(user, "type","undef");
		String source_id=JsonUtil.getStringFromJson(user, "id","");
		Json source =Json.object().set("type",type).set("id", source_id);


		if ((op>=1)&&(op<=3))  {
			if ((op==2)||(op==3)) {
				if (!value.isString())
					return Json.object().set("result", "").set("error", "value must be a string for props list");
			}
			opOnManagedProp(op, props, path, value,source );
		}
		else if (op==4) {
			undoLastOpOnManagedProp(props, path, source);
		}
		else if (op==5) {
			undoAllOpOnManagedProp(props, path, source);
		}
		else if (op==6) {
			Json p= props.get(path);
			if (p==null) return Json.object().set("result", "").set("error", "invalid prop name");
			return p;
		}

		return Json.object("result", "ok");
	}

	// propname #id.name
	// number.name


	// NOT USED
	public String getLinkProp(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst, String id, String propName)
			throws DevicesDBException {

		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);
		if (l == null)
			throw new DevicesDBException("Cannot find link to delete between" + uuidOrIpSrc + "  " + uuidOrIpDst);

		if (id.startsWith("#")) {
			for (Json j : l.get(PROPS_LIST).asJsonList()) {
				Json key = j.get(_ID);
				if (key != null) {
					if (key.isString()) {
						if (id.compareTo(key.asString()) == 0)
							return JsonUtil.getStringFromJson(j, propName, "#undef");
					}

				}
			}
		} else {

			// System.out.println(l.get(PROPS_LIST));
			try {

				Json jp = JsonUtil.getElement(l.get(PROPS_LIST), id);
				if (jp == null)
					return "#undef";
				return JsonUtil.getStringFromJson(jp, propName, "#undef");

			} catch (JsonException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return "#undef";

	}

	/*
	 * public void setLinkProp(boolean modeIp, String uuidOrIpSrc, String
	 * uuidOrIpDst, String id, String propName, String value) throws
	 * DevicesDBException {
	 * 
	 * Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst); if (l == null) throw new
	 * DevicesDBException("Cannot find link to delete between" + uuidOrIpSrc + "  "
	 * + uuidOrIpDst);
	 * 
	 * setLinkProp(l, id, propName, value);
	 * 
	 * }
	 * 
	 * public void setLinkProp(Json l, String id, String propName, String value)
	 * throws DevicesDBException {
	 * 
	 * //Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst); if (l == null) throw
	 * new DevicesDBException("Cannot find link ");
	 * 
	 * if (id.startsWith("#")) { for (Json j : l.get(PROPS_LIST).asJsonList()) {
	 * Json key = j.get(_ID); if (key != null) { if (key.isString()) { if
	 * (id.compareTo(key.asString()) == 0) return
	 * JsonUtil.getStringFromJson(j,propName,"#undef"); }
	 * 
	 * } } } else {
	 * 
	 * // System.out.println(l.get(PROPS_LIST)); try {
	 * 
	 * Json jp=JsonUtil.getElement(l.get(PROPS_LIST), id); if (jp==null) return
	 * "#undef"; return JsonUtil.getStringFromJson(jp,propName,"#undef");
	 * 
	 * } catch (JsonException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } } return "#undef";
	 * 
	 * }
	 */

	private int str2int(String s) {
		int idx = -1;

		try {
			idx = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			idx = -1;
		}
		return idx;
	}

	private Json getElementAt(Json j, String sIndex) throws DevicesDBException {

		int idx = str2int(sIndex);
		if (idx < 0)
			throw new DevicesDBException("Invalid index " + sIndex);

		if (idx < j.asJsonList().size())
			return j.at(idx);
		throw new DevicesDBException("Index out of bounds " + sIndex);
	}

	private String getFirstPathElement(String path) {
		if (path == null)
			return "";
		int n = path.indexOf('.');
		if (n < 0)
			return path;
		return path.substring(0, n);
	}

	public Json setLinkProps(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst, String id, Json value)
			throws DevicesDBException {

		Json result = Json.object();

		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);
		if (l == null)
			throw new DevicesDBException("Cannot find link to update between" + uuidOrIpSrc + "  " + uuidOrIpDst);

		if (debug)
			System.out.println("SET_LINK_PROPS path=" + id);
		if (id.startsWith("#")) {
			Json listProps = Json.array();

			for (Json j : l.get(PROPS_LIST).asJsonList()) {
				Json key = j.get(_ID);
				if (debug)
					System.out.println("ID:" + key);
				if (key != null) {
					if (key.isString()) {

						if (id.compareTo(key.asString()) != 0)
							listProps.add(j);
						else {
							value.set(_ID, key);
							listProps.add(value);
							result = value;
						}
					}

				}
			}

			l.set(PROPS_LIST, listProps);
		} else {

			if (id.startsWith("*")) {
				throw new DevicesDBException("Invalid path to setLinkProps " + uuidOrIpSrc + "  " + uuidOrIpDst);
			}

			// System.out.println(l.get(PROPS_LIST));
			try {
				String sn = getFirstPathElement(id);
				Json elt = getElementAt(l.get(PROPS_LIST), sn);
				Json _jid = elt.get(_ID);

				JsonUtil.setElement(l.get(PROPS_LIST), id, value, true);
				elt = getElementAt(l.get(PROPS_LIST), sn);
				elt.set(_ID, _jid);

				return elt;
			} catch (JsonException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;

	}

	public Json delLinkProps(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst, String id)
			throws DevicesDBException {

		Json p = Json.object();
		boolean found = true;

		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);
		if (l == null)
			throw new DevicesDBException("Cannot find link to delete between" + uuidOrIpSrc + "  " + uuidOrIpDst);

		int n = str2int(id);

		if (n >= 0) {
			List<Json> listOfProps = l.get(PROPS_LIST).asJsonList();
			if (n < listOfProps.size()) {
				p = listOfProps.get(n);
				listOfProps.remove(n);
				return p;
			} else
				throw new DevicesDBException("Index out of bounds " + n + " size:" + listOfProps.size());
		} else {
			Json listProps = Json.array();
			for (Json j : l.get(PROPS_LIST).asJsonList()) {
				Json key = j.get(_ID);
				if (key != null) {
					if (key.isString()) {
						if (id.compareTo(key.asString()) != 0) {
							listProps.add(j);
						} else {
							p = j;
							found = true;
						}
					}
				}
			}

			l.set(PROPS_LIST, listProps);
			if (!found)
				throw new DevicesDBException("Invalid id " + id);
		}

		return p;
	}

	private boolean idExists(Json propsList, String id) {

		for (Json p : propsList.asJsonList()) {
			if (id.compareTo(p.get(_ID).asString()) == 0)
				return true;
		}
		return false;
	}

	private String findNewId(Json propsList) {
		int n = propsList.asJsonList().size();

		while (idExists(propsList, "#" + n)) {
			n = n + 1;
		}
		return "#" + n;
	}

	public Json addLinkProps(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst, Json props)
			throws DevicesDBException {

		if (debug)
			System.out.println("ADDLINK PROP:" + uuidOrIpSrc + "  " + uuidOrIpDst);
		Json l = findLink(modeIp, uuidOrIpSrc, uuidOrIpDst);
		if (l == null) {
			l = addLink(modeIp, uuidOrIpSrc, uuidOrIpDst, null);

		}

		Json propsList = l.get(PROPS_LIST);

		props.set(_ID, findNewId(propsList));
		propsList.add(props);

		return props;

	}

	private Json removeTag(Json list, String tag) {

		Json newlist = Json.array();
		for (Json j : list.asJsonList()) {
			if (tag.compareTo(j.asString()) != 0)
				newlist.add(j);
		}
		return newlist;
	}

	private void delAllLinks(boolean modeIp, String id) throws DevicesDBException {

		List<String> list = new ArrayList<String>();

		String uuidSrc = "";
		if (modeIp)
			uuidSrc = dictIPtoUUID.get(id);
		else
			uuidSrc = id;
		if (uuidSrc == null)
			throw new DevicesDBException("Invalid ip src in del link " + id);

		Json deviceSrc = dictUUIDToDevice.get(uuidSrc);

		for (Json j : deviceSrc.get(LINKS_FROM).asJsonList()) {
			list.add(j.asString());
		}
		for (Json j : deviceSrc.get(LINKS_TO).asJsonList()) {
			list.add(j.asString());
		}

		for (String s : list) {
			Json link = dictUUIDToLink.get(s);
			delLink(link);
		}

	}

	private void delLink(Json link) throws DevicesDBException {

		if (debug)
			System.out.println("DEL LINK:" + linkToString(link));
		String uuid_link = link.get(UUID_TAG).asString();
		dictUUIDToLink.remove(uuid_link);
		links.asJsonList().remove(link);

		String uuidSrc = link.get(SRC_UUID).asString();
		Json deviceSrc = dictUUIDToDevice.get(uuidSrc);
		if (deviceSrc == null)
			throw new DevicesDBException("Invalid uuid src in addlink " + uuidSrc);
		Json linkSrc = deviceSrc.get(LINKS_FROM);
		if (linkSrc == null) {
			linkSrc = Json.array();
			deviceSrc.set(LINKS_FROM, linkSrc);
		}

		String uuidDst = link.get(DST_UUID).asString();
		Json deviceDst = dictUUIDToDevice.get(uuidDst);
		if (deviceDst == null)
			throw new DevicesDBException("Invalid uuid dst in addlink " + uuidDst);
		Json linkDst = deviceDst.get(LINKS_TO);
		if (linkDst == null) {
			linkDst = Json.array();
			deviceDst.set(LINKS_TO, linkDst);
		}

		deviceSrc.set(LINKS_FROM, removeTag(linkSrc, uuid_link));
		deviceDst.set(LINKS_TO, removeTag(linkDst, uuid_link));

	}

	public Json delLink(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst) throws DevicesDBException {

		String uuidSrc = "";
		if (modeIp)
			uuidSrc = dictIPtoUUID.get(uuidOrIpSrc);
		else
			uuidSrc = uuidOrIpSrc;
		if (uuidSrc == null)
			throw new DevicesDBException("Invalid ip src in del link " + uuidOrIpSrc);

		String uuidDst = "";
		if (modeIp)
			uuidDst = dictIPtoUUID.get(uuidOrIpDst);
		else
			uuidDst = uuidOrIpDst;
		if (uuidDst == null)
			throw new DevicesDBException("Invalid ip dst in del link " + uuidOrIpDst);

		Json deviceSrc = dictUUIDToDevice.get(uuidSrc);
		if (deviceSrc == null)
			throw new DevicesDBException("Invalid uuid src in addlink " + uuidSrc);

		Json deviceDst = dictUUIDToDevice.get(uuidDst);
		if (deviceDst == null)
			throw new DevicesDBException("Invalid uuid dst in addlink " + uuidDst);

		Json linkSrc = deviceSrc.get(LINKS_FROM);
		if (linkSrc == null) {
			linkSrc = Json.array();
			deviceSrc.set(LINKS_FROM, linkSrc);
		}

		Json linkDst = deviceDst.get(LINKS_TO);
		if (linkDst == null) {
			linkDst = Json.array();
			deviceDst.set(LINKS_TO, linkDst);
		}

		Json l = findLink(deviceSrc, deviceDst);
		if (l == null)
			throw new DevicesDBException("Cannot find link to delete bewteen" + uuidSrc + "  " + uuidDst);

		String uuid = l.get(UUID_TAG).asString();

		delLink(l);
		return l;

	}

	// public Json delAllLinks(boolean modeIp, String uuidOrIpSrc) throws
	// DevicesDBException {
	//
	// String uuidSrc="";
	// if (modeIp) uuidSrc=dictIPtoUUID.get(uuidOrIpSrc); else uuidSrc=uuidOrIpSrc;
	// if (uuidSrc==null) throw new DevicesDBException("Invalid ip src in del link
	// "+uuidOrIpSrc);
	//
	// String uuidDst="";
	// if (modeIp) uuidDst=dictIPtoUUID.get(uuidOrIpDst); else uuidDst=uuidOrIpDst;
	// if (uuidDst==null) throw new DevicesDBException("Invalid ip dst in del link
	// "+uuidOrIpDst);
	//
	// Integer n= dictUUIDToIndex.get(uuidSrc);
	// if (n==null) throw new DevicesDBException("Invalid uuid src in del link
	// "+uuidSrc);
	// Json deviceSrc =devices.asJsonList().get(n);
	//
	// n= dictUUIDToIndex.get(uuidDst);
	// if (n==null) throw new DevicesDBException("Invalid uuid dst in del link
	// "+uuidDst);
	// Json deviceDst =devices.asJsonList().get(n);
	//
	// Json linkSrc=deviceSrc.get("links");
	// if (linkSrc==null) {linkSrc=Json.array(); deviceSrc.set("links",linkSrc);}
	//
	// Json linkDst=deviceDst.get("links");
	// if (linkDst==null) {linkDst=Json.array(); deviceDst.set("links",linkDst);}
	//
	// Json l= findLink(deviceSrc, deviceDst);
	// if (l==null) throw new DevicesDBException("Cannot find link to delete
	// bewteen"+uuidSrc+" "+uuidDst);
	//
	// String uuid=l.get("uuid").asString();
	// dictUUIDToLink.remove(uuid);
	//
	// deviceSrc.set("links", removeTag(linkSrc, uuid));
	// deviceDst.set("links", removeTag(linkDst, uuid));
	//
	// return l;
	//
	//
	// }

	// return link
	public Json addLink(boolean modeIp, String uuidOrIpSrc, String uuidOrIpDst, Json props) throws DevicesDBException {

		if (debug)
			System.out.println("ADD_LINK " + uuidOrIpSrc + " ==> " + uuidOrIpDst);
		String uuidSrc = "";
		if (modeIp)
			uuidSrc = dictIPtoUUID.get(uuidOrIpSrc);
		else
			uuidSrc = uuidOrIpSrc;
		if (uuidSrc == null)
			throw new DevicesDBException("Invalid ip src in add link " + uuidOrIpSrc);

		String uuidDst = "";
		if (modeIp)
			uuidDst = dictIPtoUUID.get(uuidOrIpDst);
		else
			uuidDst = uuidOrIpDst;
		if (uuidDst == null)
			throw new DevicesDBException("Invalid ip dst in add link " + uuidOrIpDst);

		if (uuidDst.compareTo(uuidSrc) == 0) {
			throw new DevicesDBException("Cannot create reflexive link  " + uuidSrc);

		}

		Json deviceSrc = dictUUIDToDevice.get(uuidSrc);
		if (deviceSrc == null)
			throw new DevicesDBException("Invalid uuid src in addlink " + uuidSrc);

		Json deviceDst = dictUUIDToDevice.get(uuidDst);
		if (deviceDst == null)
			throw new DevicesDBException("Invalid uuid dst in addlink " + uuidDst);

		Json linkSrc = deviceSrc.get(LINKS_FROM);
		if (linkSrc == null) {
			linkSrc = Json.array();
			deviceSrc.set(LINKS_FROM, linkSrc);
		}

		Json linkDst = deviceDst.get(LINKS_TO);
		if (linkDst == null) {
			linkDst = Json.array();
			deviceDst.set(LINKS_TO, linkDst);
		}

		Json l = findLink(deviceSrc, deviceDst);
		if (l != null) {
			throw new DevicesDBException("Link betwÒeen " + uuidSrc + "(" + deviceSrc.get(IP) + ") and " + uuidDst + "("
					+ deviceDst.get(IP) + ") already created");
		}

		l = Json.object();
		l.set(SRC_UUID, uuidSrc);
		l.set(DST_UUID, uuidDst);
		if (props == null)
			props = Json.object();
		Json propsList = Json.array();
		propsList.add(props);
		props.set(_ID, "#0");
		l.set(PROPS_LIST, propsList);

		UUID uuidx = UUID.randomUUID();
		String uuid = uuidx.toString();
		l.set(UUID_TAG, uuid);
		links.add(l);
		dictUUIDToLink.put(uuid, l);

		deviceSrc.get(LINKS_FROM).add(uuid);
		deviceDst.get(LINKS_TO).add(uuid);

		return l;
	}

	private Json doCreateLink(String uuidSrc, String uuidDst, Json props) {
		Json l = Json.object();
		l.set(SRC_UUID, uuidSrc);
		l.set(DST_UUID, uuidDst);
		if (props == null)
			props = Json.object();
		Json propsList = Json.array();
		propsList.add(props);
		props.set(_ID, "#0");
		l.set(PROPS_LIST, propsList);

		UUID uuidx = UUID.randomUUID();
		String uuid = uuidx.toString();
		l.set(UUID_TAG, uuid);
		links.add(l);
		dictUUIDToLink.put(uuid, l);

		return l;

	}

	public void loadFiles() {

		if (CSLContext.instance.getDatabaseServer().docExists(FILE_NAME_DEVICES)) {
			Json j = CSLContext.instance.getDatabaseServer().loadDataFileAsJson(FILE_NAME_DEVICES);
			if (j.has(DEVICES_LIST))
				devices = j.get(DEVICES_LIST);
			else {
				System.err.println("Devices not found in doc");
				devices = Json.array();
			}
			if (j.has(LINKS_LIST))
				links = j.get(LINKS_LIST);
			else {
				System.err.println("Links not found in doc");
				links = Json.array();
			}
			if (j.has(NETWORK_VUES_LIST))
				networkVues = j.get(NETWORK_VUES_LIST);
			else {
				// System.err.println("Links not found in doc");
				networkVues = Json.array();
			}
		} else {
			devices = Json.array();
			links = Json.array();
			networkVues = Json.array();
		}

		for (Json device : devices.asJsonList()) {
			String uuid = "";
			if (device.has(UUID_TAG))
				uuid = device.get(UUID_TAG).asString();
			else {
				UUID uuidx = UUID.randomUUID();
				uuid = uuidx.toString();
				updated = true;
				device.set(UUID_TAG, uuid);
			}
			if (!device.has(IP)) {
				System.err.println("Invaild device descriptor (no ip):" + device);
			} else {
				String ip = device.get(IP).asString();
				if (dictIPtoUUID.get(ip) == null) {
					addToDict(device.get(IP).asString(), device.get(UUID_TAG).asString(), device);

				} else {
					System.err.println("Multiple devices with same ip :" + device);
				}
			}

		}

		for (Json link : links.asJsonList()) {
			String uuid = "";
			if (link.has(UUID_TAG)) {
				uuid = link.get(UUID_TAG).asString();
				dictUUIDToLink.put(uuid, link);
			} else {
				System.err.println("Invalid link uuid:" + linkToString(link));
			}
		}

	}

	public void saveFiles() {

		Json j = Json.object();
		j.set(DEVICES_LIST, devices);
		j.set(LINKS_LIST, links);
		j.set(NETWORK_VUES_LIST, networkVues);
		// System.out.println(j);

		CSLContext.instance.getDatabaseServer().saveJsonAsDataFile(FILE_NAME_DEVICES, Json.object().set("contents", j), true);
		updated = false;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}

	private String getIp(String uuid) {
		Json d = dictUUIDToDevice.get(uuid);
		if (d == null)
			return "#No device";
		Json s = d.get(IP);
		if (s == null)
			return "#No ip";
		return s.toString();
	}

	private String linkToString(Json l) {
		String src = getIp(l.get(SRC_UUID).asString());
		String dst = getIp(l.get(DST_UUID).asString());

		return src + "->" + dst + ":" + l;
	}

	public void dump() {

		System.out.println("DUMP");

		prettyPrint(devices);

		System.out.println("Devices");
		for (Json d : devices.asJsonList()) {
			System.out.println("  " + d);
		}
		System.out.println("Links");

		for (Json l : links.asJsonList()) {
			System.out.println("  " + linkToString(l));
		}
		System.out.println("IP to UUID");

		for (Map.Entry me : dictIPtoUUID.entrySet()) {
			System.out.println("ip: " + me.getKey() + " ==> " + me.getValue());
		}
		System.out.println("Index");
		for (Map.Entry me : dictUUIDToDevice.entrySet()) {
			System.out.println("idx: " + me.getKey() + " ==> " + me.getValue());
		}

		System.out.println("Links");
		for (Map.Entry me : dictUUIDToLink.entrySet()) {
			System.out.println("link: " + me.getKey() + " ==> " + me.getValue());
		}

		System.out.println("END DUMP");


	}

	public String map2String(HashMap map) {

		StringBuilder mapAsString = new StringBuilder("{");
		for (Object key : map.keySet()) {
			mapAsString.append(key + "=" + map.get(key) + ", ");
		}
		mapAsString.delete(mapAsString.length() - 2, mapAsString.length()).append("}");
		return mapAsString.toString();

	}

	public String map2Key(HashMap map) {

		StringBuilder mapAsString = new StringBuilder("{");
		for (Object key : map.keySet()) {
			mapAsString.append(key + ", ");
		}
		mapAsString.delete(mapAsString.length() - 2, mapAsString.length()).append("}");
		return mapAsString.toString();

	}

	public Json findDeviceWithhUUID(String uuid) {

		Json device = dictUUIDToDevice.get(uuid);
		return device;
	}

	public List<String> checkIntegrity() {
		List<String> errors = new ArrayList<String>();

		for (Json jdevice : devices.asJsonList()) {
			Json links1 = jdevice.get(LINKS_FROM);

			if (links1 != null) {
				for (Json luuid : links1.asJsonList()) {
					String key = luuid.asString();
					if (dictUUIDToLink.get(key) == null) {
						errors.add(
								"the (from)link " + key + " in the device " + jdevice.get(UUID_TAG) + " is invalid ");
						errors.add(map2Key(dictUUIDToLink));
					}
				}
			}
			Json links2 = jdevice.get(LINKS_TO);
			if (links2 != null) {
				for (Json luuid : links2.asJsonList()) {
					String key = luuid.asString();
					if (dictUUIDToLink.get(key) == null) {
						errors.add("the (to)link " + key + " in the device " + jdevice.get(UUID_TAG) + " is invalid ");
						errors.add(map2Key(dictUUIDToLink));
					}
				}
			}
		}

		for (Json l : links.asJsonList()) {
			// get src
			String src_uuid = l.get(SRC_UUID).asString();
			String dst_uuid = l.get(DST_UUID).asString();

			String link_uuid = l.get(UUID_TAG).toString();

			// devices.get(property)
			// check this uid in src
			Json device_src = dictUUIDToDevice.get(src_uuid);
			if (device_src == null)
				errors.add("the link " + link_uuid + " has an invalid src " + src_uuid);
			Json device_dst = dictUUIDToDevice.get(dst_uuid);
			if (device_dst == null)
				errors.add("the link " + link_uuid + " has an invalid dst " + dst_uuid);

			if (!findFromLink(device_src, link_uuid)) {
				errors.add("the link " + link_uuid + "not found in src " + src_uuid);
			}

			if (!findToLink(device_dst, link_uuid)) {
				errors.add("the link " + link_uuid + "not found in dst " + dst_uuid);
			}

		}

		return errors;
	}

	public static void setTestModel1() {
		DevicesDB2 z=DevicesDB2.instance;

		z.clear();

		try {
			Json d = z.createDevice(false, "", Json.object().set(IP, "1.1.1.1"));
			Json d2 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.2"));
			Json d3 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.3"));
			for (int i = 4; i < 10; i++)
				z.createDevice(true, "1.1.1." + i, null);

			Json d4 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.67"));

			//z.dump();

			z.updateDevice(true, "1.1.1.2", "props.os", Json.make("windows"));
			z.updateDevice(false, d.get(UUID_TAG).asString(), MACS, Json.make("mac1"));
			z.updateDevice(false, d.get(UUID_TAG).asString(), MACS, Json.make("mac2"));
			//z.dump();

			z.addLink(true, "1.1.1.1","1.1.1.3",null);
			//	z.setLinkProps(true, "1.1.1.1","1.1.1.3", "#0" , value)

		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public static void test2() {

		DevicesDB2 z = new DevicesDB2();

		Json user = Json.object().set("name", "test");
		// result = devicesDB.exec(user, modeIp,op, id, id2, path, value, null);

		try {
			Json x = z.exec(user, true, "GET_LINK", "178.12.13.3", "2.2.2.2", "", null, null);

			System.out.println("Result=" + x);
		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void updateLearnedModel(IDSLearnedRules model) {

		// DevicesDB z = DevicesDB.instance;

		for (Json j : getListOfDevices()) {

			String uuid = j.get(UUID_TAG).asString();

			System.out.println("*** Device=" + j);

			Json jdev = Json.object();

			jdev.set("ip", j.get("ip").asString());
			jdev.set("macs", j.get("macs"));

			System.out.println(">>" + jdev);
			int v = -1;
			try {

				Json vp = getDeviceProp(false, j.get(UUID_TAG).asString(), "props.vul_level");
				if (vp != null)
					v = vp.asInteger();
			} catch (DevicesDBException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			jdev.set("severity", new SeverityLevel(v).getLevelAsString());

			model.updateFromDBDevice("update_ip", jdev);

			try {
				Json links = getLinksFrom(false, uuid);

				for (Json jlink : links.asJsonList()) {
					System.out.println("   -" + jlink);

					String uuid_dst = jlink.get(DST_UUID).asString();

					Json deviceDst = findDeviceWithhUUID(uuid);

					if (deviceDst != null) {
						String ip_dst = deviceDst.get(IP).asString();

						for (Json jlinkflow : jlink.get(PROPS_LIST).asJsonList()) {
							System.out.println("      " + jlinkflow);
							Json jparams = Json.object();
							jparams.set("ip_src", j.get("ip"));
							jparams.set("port_src", JsonUtil.getIntFromJson(jlinkflow, "port_src", -1));
							jparams.set("ip_dst", ip_dst);
							jparams.set("port_dst", JsonUtil.getIntFromJson(jlinkflow, "port_dst", -1));
							System.out.println("sprot:" + JsonUtil.getStringFromJson(jlinkflow, "protocol", ""));
							int prot = PacketInfo.protocolNameToInt(JsonUtil.getStringFromJson(jlink, "protocol", ""));
							jparams.set("protocol", prot);
							int anomaly = JsonUtil.getIntFromJson(jlinkflow, "anomaly_level", -1);
							if (anomaly >= 0) {
								RiskLevel r = new RiskLevel(anomaly);
								jparams.set("severity", r.getSeverityLevel().getLevelAsString());
								jparams.set("likelihood", r.getLikelyhoodLevel().getLevelAsString());
							}

							System.out.println(">>>" + jparams);

							model.updateFromDBDevice("update_link", jparams);

						}
					}
				}

			} catch (DevicesDBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/*
	 * devices setConfidenceLevel(value, source) addMac(value, source)
	 * 
	 * histo: propname mode : add or set [source, source ...] source: {type:'manu",
	 * 'ra', 'learn', 'al'] (manual, riks analysis, learning, alert id:"x12scjksd"
	 * // id of the source if relevant ts: // time stamp operation
	 * setManagedProp(json, name, value, source), cancelSetManaged(json, name,
	 * value, default); set to default or remove from the liste
	 * getManagedPropValue(json,name)
	 *
	 * setManagedPropList(json, name, value, op), // op 'add' or 'del'
	 * unsetManagedPropList(json, name, source) //undo the last source action
	 * 
	 * getManagedPropList
	 * 
	 * 
	 * links setAnomalyLevel(value, source)
	 * 
	 */

	static String HISTO = "__histo";
	static String MODE = "il"; // acces mode
	static String SOURCE = "source";
	static String PROP_DESC = "pdesc";
	static String SOURCES_LIST = "sl";
	static String TIME_STAMP = "ts";
	static String PREV_VALUE = "pv";
	static String NEW_VALUE = "nv";

	static String SOURCE_TYPE = "type";
	static String SOURCE_ID = "id";
	static String SOURCE_OP = "op";

	// static String DEFAULT_ACCESS_MODE="set";
	// static String SET_ACCESS_MODE="set";
	// static String ADD_ACCESS_MODE="add";

	static public void createManagedProp(Json j, String name, boolean isList) {

		// if ( ( mode.compareToIgnoreCase(SET_ACCESS_MODE)!=0 &&
		// mode.compareToIgnoreCase(ADD_ACCESS_MODE)!=0) ) {
		// mode=SET_ACCESS_MODE;
		// }

		if (!j.has(HISTO))
			j.set(HISTO, Json.object());
		Json histo = j.get(HISTO);

		if (!histo.has(name)) {
			histo.set(name, Json.object().set(MODE, isList).set(SOURCES_LIST, Json.array()));
		}
		Json pdesc = histo.get(name);

		// boolean
		// setAccessMode=SET_ACCESS_MODE.compareToIgnoreCase(JsonUtil.getStringFromJson(pdesc,
		// MODE, DEFAULT_ACCESS_MODE))==0;

		if (!j.has(name)) {
			if (!isList)
				j.set(name, Json.make(""));
			else
				j.set(name, Json.array());
		}
	}

	static public int opCodeForManagedProp(String s  ) {
		if ("set".compareToIgnoreCase(s)==0) return 1;
		if ("add".compareToIgnoreCase(s)==0) return 2;
		if ("del".compareToIgnoreCase(s)==0) return 3;
		if ("undo_last".compareToIgnoreCase(s)==0) return 4;
		if ("undo_all".compareToIgnoreCase(s)==0) return 5;
		if ("get".compareToIgnoreCase(s)==0) return 6;

		return -1;
	}

	static public void opOnManagedProp(int op, Json j, String name, String svalue, Json source0) {

		Json jvalue=Json.make(svalue);
		opOnManagedProp(op, j, name, jvalue, source0);

	}
	// op: 1 set
	// 2 add
	// 3 del
	//


	static public void opOnManagedProp(int op, Json j, String name, Json  jvalue, Json source0) {

		if ((op < 1) || (op > 3))
			return;
		boolean isList = (op != 1);

		if (!j.has(HISTO))
			j.set(HISTO, Json.object());
		Json histo = j.get(HISTO);

		if (!histo.has(name)) {
			histo.set(name, Json.object().set(MODE, isList).set(SOURCES_LIST, Json.array()));
		}
		Json pdesc = histo.get(name);

		Json source = Json.object();
		source.set(SOURCE_TYPE, JsonUtil.getStringFromJson(source0, SOURCE_TYPE, "???"));
		source.set(SOURCE_ID, JsonUtil.getStringFromJson(source0, SOURCE_ID, ""));

		// boolean
		// setAccessMode=SET_ACCESS_MODE.compareToIgnoreCase(JsonUtil.getStringFromJson(pdesc,
		// MODE, DEFAULT_ACCESS_MODE))==0;



		Json prev = null;
		Json newValue = null;

		if (!j.has(name)) {
			if (!isList)
				j.set(name, Json.object());
			else
				j.set(name, Json.array());
		}
		/*else {
				if (!isList)
					newValue = jvalue;
				else
					newValue = jvalue;
			}*/
		newValue=jvalue;

		// convert scalar to list
		if (!j.get(name).isArray()&&((op==2)||(op==3))) {
			Json array=Json.array();
			array.add(j.get(name));
			j.set(name, array);
		}

		String svalue="";
		if ((op==2)||(op==3)) {
			svalue=jvalue.asString();
		}
		else 
			svalue=jvalue.toString(); // serialize objects

		String sop = "";
		if (op == 1)
			sop = "set";
		if (op == 2)
			sop = "add";
		if (op == 3)
			sop = "del";

		delAllModificationBySourceWithThisValue(j, name, source, sop, svalue);

		if (op == 1)
			j.set(name, jvalue);
		else if (op == 2) {

			if (!listContainsValue(j.get(name).asJsonList(), svalue))
				j.get(name).add(svalue);

		} else if (op == 3)
			j.get(name).remove(svalue);

		source.set(TIME_STAMP, TimeUtil.timeStampNano());
		//if (prev != null)
		//	source.set(PREV_VALUE, prev);
		if (newValue != null)
			source.set(NEW_VALUE, svalue);
		if (op == 1)
			source.set(SOURCE_OP, "set");
		if (op == 2)
			source.set(SOURCE_OP, "add");
		if (op == 3)
			source.set(SOURCE_OP, "del");

		pdesc.get(SOURCES_LIST).add(source);

	}

	static public boolean listContainsValue(List<Json> list, String value) {

		for (Json j : list) {
			if (j.asString().compareTo(value) == 0)
				return true;
		}
		return false;
	}

	static public boolean sourcesEqual(Json source1, Json source2) {

		String type1 = JsonUtil.getStringFromJson(source1, "type", "undef");
		if (type1.compareTo("undef") == 0)
			return false;
		String type2 = JsonUtil.getStringFromJson(source2, "type", "undef");
		if (type2.compareTo("undef") == 0)
			return false;
		if (type1.compareTo(type2) != 0)
			return false;

		String id1 = JsonUtil.getStringFromJson(source1, "id", "");
		String id2 = JsonUtil.getStringFromJson(source2, "id", "");

		return id1.compareTo(id2) == 0;

	}

	static public int findIndexOfLastModificationBySource(Json j, String name, Json source) {

		if (!j.has(HISTO))
			return -1;
		Json histo = j.get(HISTO);

		if (!histo.has(name))
			return -1;
		Json listOfSources = histo.get(name).get(SOURCES_LIST);

		List<Json> l = listOfSources.asJsonList();
		int i = l.size() - 1;

		while (i >= 0) {
			Json source_i = l.get(i);
			if (sourcesEqual(source, source_i))
				return i;
			i--;
		}
		return -1;
	}

	static String source2str(Json source) {
		return "" + source.get(SOURCE_TYPE).asString() + "[" + source.get(SOURCE_ID).asString() + "]"
				+ source.get(TIME_STAMP).asString();
	}

	static public void reBuildList(Json j, String name, Json source) {

		if (!j.has(HISTO))
			return;
		Json histo = j.get(HISTO);

		if (!histo.has(name))
			return;
		Json listOfSources = histo.get(name).get(SOURCES_LIST);

		j.get(name).asJsonList().clear();

		List<Json> l = listOfSources.asJsonList();

		for (int i = 0; i < l.size(); i++) {
			Json source_i = l.get(i);
			// if (sourcesEqual(source, source_i))
			{
				Json jvalue = source_i.get(NEW_VALUE);
				String value = jvalue.asString();
				String op = source_i.get(SOURCE_OP).asString();
				if (op.compareTo("add") == 0) {
					if (!j.get(name).asJsonList().contains(jvalue))
						j.get(name).add(value);
				}
				if (op.compareTo("del") == 0)
					j.get(name).remove(value);

			}
			System.out.println("Rebuild " + name + " source=" + source2str(source_i) + ':' + j.get(name));
		}

	}

	static public void delAtIndexModificationBySource(Json j, String name, int idx) {

		if (idx < 0) {
			System.err.println("Invalid idx");
		}
		if (!j.has(HISTO))
			return;
		Json histo = j.get(HISTO);

		if (!histo.has(name))
			return;
		Json listOfSources = histo.get(name).get(SOURCES_LIST);

		List<Json> l = listOfSources.asJsonList();
		l.remove(idx);
	}

	static public void delAllModificationBySource(Json j, String name, Json source) {

		if (!j.has(HISTO))
			return;
		Json histo = j.get(HISTO);

		if (!histo.has(name))
			return;
		Json listOfSources = histo.get(name).get(SOURCES_LIST);

		Json newList = Json.array();

		int i = 0;
		while (i < listOfSources.asJsonList().size()) {
			if (sourcesEqual(listOfSources.asJsonList().get(i), source)) {
				listOfSources.asJsonList().remove(i);
			} else
				i++;
		}

	}



	static public void delAllModificationBySourceWithThisValue(Json j, String name, Json source, String op,
			String svalue) {

		if (!j.has(HISTO))
			return;
		Json histo = j.get(HISTO);

		if (!histo.has(name))
			return;
		Json listOfSources = histo.get(name).get(SOURCES_LIST);

		Json newList = Json.array();

		//String svalue=value.toString();

		int i = 0;
		while (i < listOfSources.asJsonList().size()) {
			boolean removed = false;

			if (sourcesEqual(listOfSources.asJsonList().get(i), source)) {
				String op2 = listOfSources.asJsonList().get(i).get(SOURCE_OP).asString();
				Json xxx=listOfSources.asJsonList().get(i);
				String value2 = listOfSources.asJsonList().get(i).get(NEW_VALUE).asString();
				if ((op.compareToIgnoreCase(op2) == 0) && (svalue.compareToIgnoreCase(value2) == 0)) {
					listOfSources.asJsonList().remove(i);
					removed = true;
				}

			}
			if (!removed)
				i++;
		}

	}

	// do not check histo (to be used if idx >0
	static public Json getSourceModificationAtIndex(Json j, String name, int idx) {
		List<Json> list = j.get(HISTO).get(name).get(SOURCES_LIST).asJsonList();
		if (idx < 0)
			return null;
		if (idx > list.size() - 1)
			return null;
		return list.get(idx);
	}

	// do not check histo (to be used if idx >0
	static public int getSourceModificationsSize(Json j, String name) {
		List<Json> list = j.get(HISTO).get(name).get(SOURCES_LIST).asJsonList();

		return list.size();
	}

	static String pad(String s, int n) {

		while (s.length() < n)
			s = s + "              ";
		return s.substring(0, n);
	}

	// ajouter un mode all , on supprime tous les éléments de la source

	static public void undoLastOpOnManagedProp(Json j, String name, Json source) {
		undoOpOnManagedProp(j, name, source, true);
	}

	static public void undoAllOpOnManagedProp(Json j, String name, Json source) {
		undoOpOnManagedProp(j, name, source, false);

	}

	static private void undoOpOnManagedProp(Json j, String name, Json source, boolean last) {

		if (!j.has(HISTO))
			return;
		Json histo = j.get(HISTO);

		if (!histo.has(name))
			return;
		Json pdesc = histo.get(name);

		boolean isList = JsonUtil.getBooleanFromJson(pdesc, MODE, false);

		if (!isList) {
			if (last) {
				int idx = findIndexOfLastModificationBySource(j, name, source);
				if (idx < 0)
					return;
				delAtIndexModificationBySource(j, name, idx);
			} else {
				delAllModificationBySource(j, name, source);

			}

			String svalue = "";
			int size = getSourceModificationsSize(j, name);
			if (size > 0) {
				svalue = getSourceModificationAtIndex(j, name, size - 1).get(NEW_VALUE).asString();
				try {
					Json obj=Json.read(svalue);
					j.set(name, obj);
				} catch (Exception e) {
					System.err.println(e);
					System.err.println("Value="+svalue);
				}
			}
			else {
				j.set(name, "???");
			}

		} else {
			if (last) {
				int idx = findIndexOfLastModificationBySource(j, name, source);
				if (idx < 0)
					return;
				delAtIndexModificationBySource(j, name, idx);
			} else {
				delAllModificationBySource(j, name, source);
			}

			reBuildList(j, name, source);
		}

	}

	static public String dumpHisto(Json j, String decal) {
		if (!j.has(HISTO))
			return "";
		Json histo = j.get(HISTO);

		String s = "";
		for (Map.Entry<String, Json> entry : histo.asJsonMap().entrySet()) {
			String key = entry.getKey();
			Json je = entry.getValue();
			if (key.length() < 10)
				key = (key + "           ").substring(0, 10);
			boolean isList = JsonUtil.getBooleanFromJson(je, "il", false);
			if (isList)
				key = key + "*:";
			else
				key = key + " :";

			for (Json source : je.get(SOURCES_LIST)) {
				if (!s.isEmpty())
					s = s + "\n";

				String z = "" + source.get(SOURCE_TYPE).asString() + " [" + pad(source.get(SOURCE_ID).asString(), 8)
				+ "] " + source.get(SOURCE_OP) + " " + source.get(TIME_STAMP).asString() + " value="
				+ source.get(NEW_VALUE);

				s = s + decal + key + z;
			}
		}

		return s;
	}

	static String dumpManagedObject(Json j) {

		String result = "Props:";
		for (Map.Entry<String, Json> entry : j.asJsonMap().entrySet()) {
			String key = entry.getKey();
			Json je = entry.getValue();
			if (key.compareTo(HISTO) != 0) {
				if (!result.isEmpty())
					result = result + "\n";
				result = result + " " + pad(key, 8) + ":";
				if (je.isArray()) {
					String z = "";
					for (Json e : je.asJsonList()) {
						if (!z.isEmpty())
							z = z + ",";
						z = z + e.toString();
					}
					result = result + "[" + z + "]";
				} else
					result = result + je.toString();
			}

		}

		result = result + "\n --------------------------------------------------------\n";
		result = result + dumpHisto(j, " ");
		return result;
	}

	public static void test3() {

		DevicesDB z = DevicesDB.instance;

		z.clear();
		Json user = Json.object().set("name", "test");

		if (z.isUpdated())
			z.saveFiles();
		z.dump();

		z.saveFiles();

		// enregistrer actions
		// ajouter user
		// record actions (utilsiatble pour roolback, ajouter oldValue)

		z.clear();
		try {
			z.exec(user, true, ADD_DEVICE, "1.1.1.1", "", "", null, null);
			z.exec(user, true, ADD_DEVICE, "1.1.1.2", "", "", null, null);

			Json props = Json.object();
			props.set("port_src", -1);
			props.set("port_dst", 502);
			props.set("protocol", "TCP");
			props.set("app_protocol", "modbus");
			props.set("likelihood", "HIGH");
			props.set("severity", "MEDIUM");
			props.set("risk", "HIGH");

			props.set("size", 0);
			props.set("packets", 0);

			props.set("maxKBpersec", 0);
			props.set("maxpktpersec", 0);

			props.set("first_time", System.currentTimeMillis());
			props.set("last_time", System.currentTimeMillis());

			Json p2 = Json.object();
			p2.set("xxx", 2);

			System.out.println(JsonUtil.concat(props, p2));

			Set<String> keys = props.asJsonMap().keySet();
			keys.toArray(new String[keys.size()]);

			System.out.println(props.asJsonMap());

			z.exec(user, true, ADD_LINK, "1.1.1.1", "1.1.1.2", "", props, null);

			Json j1 = z.exec(user, true, GET_LINK_BETWEEN, "1.1.1.2", "1.1.1.1", "", null, null);
			Json j2 = z.exec(user, true, GET_LINK_BETWEEN, "1.1.1.1", "1.1.1.2", "", null, null);

			System.out.println(j1);
			System.out.println(j2);

			Json pselect = Json.object();
			pselect.set("protocol", "TCP");
			Json op = z.exec(user, true, SELECT_AND_UPDATE_LINK_BETWEEN, "1.1.1.1", "1.1.1.2", "", pselect, props);
			System.out.println(op);

			pselect.set("protocol", "UDP");
			op = z.exec(user, true, SELECT_AND_UPDATE_LINK_BETWEEN, "1.1.1.1", "1.1.1.2", "", pselect, props);
			System.out.println(op);



			// z.dump();

		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		z.saveFiles();

	}

	static public Json source(String type, String id) {
		return Json.object().set(SOURCE_TYPE, type).set(SOURCE_ID, id);
	}

	static String prettyPrint(Json j) {
		System.out.println(j);
		return JsonUtil.prettyPrint2(j);
	}

	public static void test4() {
		//	String configFile = "";
		//	if (configFile.isEmpty())
		//		configFile = System.getProperty("user.dir") + File.separator + "runconfig/CSLConfigIDS.json";

		DevicesDB.FILE_NAME_DEVICES = "devices_test";

		//	Json j = CSLContext.context.setConfigFileName(configFile);

		// testUpdateModel();

		List<String> list = new ArrayList<String>();

		list.add("essai1");
		list.add("essai");
		System.out.println(list.indexOf("essai"));

		// test3();

		Json jj = Json.object().set("x", 12).set("y", 15);

		System.out.println(prettyPrint(jj));

		Json myobject = Json.object();

		createManagedProp(myobject, "x", false);
		createManagedProp(myobject, "list", true);

		opOnManagedProp(1, myobject, "x", "12", source("manu", "1"));
		opOnManagedProp(1, myobject, "x", "26", source("manu", "1"));
		opOnManagedProp(1, myobject, "x", "17", source("manu", "1"));
		opOnManagedProp(1, myobject, "x", "26", source("manu", "1"));
		opOnManagedProp(1, myobject, "x", "26", source("manu", "1"));

		System.out.println(JsonUtil.prettyPrint(myobject));
		System.out.println(prettyPrint(myobject));

		Json test = Json.array();
		test.add(Json.object().set("xxx", 1).set("y", 2222).set("z", Json.object()).set("w", Json.object()));
		test.add("xxx");
		test.add("yyy");
		test.add(Json.object());
		test.add(Json.object());
		System.out.println(test);

		System.out.println(JsonUtil.prettyPrint(test));

		System.out.println(prettyPrint(test));

		Json source1 = Json.object().set("type", "manu").set(SOURCE_ID, "121");

		Json source2 = Json.object().set("type", "manu").set(SOURCE_ID, "2");

		opOnManagedProp(2, myobject, "list", ("abc"), source1);
		opOnManagedProp(2, myobject, "list", ("def"), source1); // ("manu","234"));
		opOnManagedProp(2, myobject, "list", ("def"), source2); // ("manu","234"));
		opOnManagedProp(2, myobject, "list", ("def"), source2); // ("manu","234"));

		System.out.println(prettyPrint(myobject));
		opOnManagedProp(2, myobject, "list", ("abc"), source1);
		opOnManagedProp(3, myobject, "list", ("abc"), source1);
		opOnManagedProp(2, myobject, "list", ("lnm"), source2);

		System.out.println(prettyPrint(myobject));

		System.out.println("idx=" + findIndexOfLastModificationBySource(myobject, "x", source1));

		System.out.println("idx=" + findIndexOfLastModificationBySource(myobject, "list", source1));

		String sobj1 = myobject.toString();

		String sobj = JsonUtil.prettyPrint2(myobject);
		// delAtIndexModificationBySource(myobject, "x", 0);

		// System.out.println(dumpHisto(myobject," "));

		// delAllModificationBySource(myobject, "x", source1);
		// System.out.println("dump histo");
		// System.out.println(dumpHisto(myobject," "));

		// delAllModificationBySource(myobject, "list", source1);

		// System.out.println("dump histo");
		// System.out.println(dumpHisto(myobject, " "));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		System.out.println("============== UNDO list");

		undoLastOpOnManagedProp(myobject, "list", source1);
		System.out.println("============== unod list source1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "list", source1);
		System.out.println("============== undo list source1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoAllOpOnManagedProp(myobject, "list", source2);
		System.out.println("============== undo all list source2");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoAllOpOnManagedProp(myobject, "list", source1);
		System.out.println("============== undo all list source1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		Json jobj = Json.read(sobj);
		System.out.println(jobj);

		boolean ok = jobj.toString().compareTo(sobj1) == 0;

		System.out.println("Compare:" + ok);

		//setTestModel1();

		//DevicesDB.instance.dump();



	}

	public void config1() {

		DevicesDB2 z = DevicesDB2.instance;

		z.clear();

		try {
			Json d = z.createDevice(false, "", Json.object().set(IP, "1.1.1.1"));
			Json d2 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.2"));
			Json d3 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.3"));
			for (int i = 4; i < 10; i++)
				z.createDevice(true, "1.1.1." + i, null);

			Json d4 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.67"));


			z.delDevice(false, d3.get(UUID_TAG).asString());


			z.addLink(true, "1.1.1.1", "1.1.1.2", Json.object());

			Json link = z.findLink(true, "1.1.1.1", "1.1.1.2");

			z.setLinkProps(true, "1.1.1.1", "1.1.1.2", "#0", Json.object().set("test", "essai"));

			for (int i = 4; i < 10; i++) {
				Json p = Json.object();
				p.set("protocol", "TCP");
				p.set("risk", "low");
				p.set("app_protocol", "modbus");
				z.addLinkProps(true, "1.1.1.1", "1.1.1."+i, p);
			}
			// z.delLink(true, "1.1.1.1", "1.1.1.2");


			Json user = Json.object().set("name", "test");

			z.exec(user, true, ADD_DEVICE, "1.2.1.1", "", "", null, null);
			z.dump();
			System.out.println(z.exec(user, true, GET_DEVICE, "1.2.1.1", "", "", null, null));

			z.exec(user, true, SET_DEVICE_PROP, "1.2.1.1", "", MACS, Json.make("aaaa-bbbb"), null);
			z.exec(user, true, SET_DEVICE_PROP, "1.2.1.1", "", "props.os", Json.make("window"), null);

			Json result = z.exec(user, true, GET_DEVICE_PROP, "1.2.1.1", "", "props.os", null, null);



			for (int i = 3; i < 10; i++)
				z.exec(user, true, ADD_DEVICE, "1.2.1." + i, "", "", null, null);

			z.exec(user, true, ADD_DEVICE, "1.2.1.2", "", "", null, null);
			z.exec(user, true, ADD_LINK, "1.2.1.2", "1.2.1.1", "", null, null);

			for (int i = 3; i < 5; i++)
				z.exec(user, true, ADD_LINK, "1.2.1." + i, "1.2.1.1", "", null, null);
			for (int i = 3; i <= 5; i++)
				z.exec(user, true, ADD_LINK, "1.2.1." + i, "1.2.1.6", "", null, null);


			result = z.exec(user, true, SET_DEVICE_PROP, "1.2.1.2", "", "props.os", Json.make("linux"), null);

			for (int i = 3; i < 5; i++) {
				Json p = Json.object();
				p.set("protocole", "TCP");
				p.set("risk", "low");
				p.set("app_protocol", "modbus");
				// z.addLinkProps(true,"1.2.1."+i, "1.2.1.1", p);
				z.exec(user, true, ADD_LINK_PROPS, "1.2.1." + i, "1.2.1.1", "", p, null);

				p = Json.object();
				p.set("protocole", "UDP");
				p.set("risk", "low");
				p.set("app_protocol", "modbus");
				// z.addLinkProps(true,"1.2.1."+i, "1.2.1.1", p);
				z.exec(user, true, ADD_LINK_PROPS, "1.2.1." + i, "1.2.1.1", "", p, null);

			}


			result = z.exec(user, true, SET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "0.protocol", Json.make("test_os"), null);

			result = z.exec(user, true, GET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "#0", Json.make("test_os"), null);

			result = z.exec(user, true, GET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "0", Json.make("test_os"), null);

			result = z.exec(user, true, GET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "0.protocol", Json.make("test_os"), null);

			result = z.exec(user, true, GET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "1._id", Json.make("test_os"), null);



			result = z.exec(user, true, SET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "0.protocol", Json.make("test_os_linux"),
					null);

			result = z.exec(user, true, SET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "#0.protocol",
					Json.make("test_os_linux2"), null);

			result = z.exec(user, true, GET_LINK, "1.2.1.3", "1.2.1.1", "", null, null);

			result = z.exec(user, true, GET_LINKS_FROM, "1.2.1.3", "", "", null, null);
			result = z.exec(user, true, GET_LINKS_TO, "1.2.1.1", "", "", null, null);


			result = z.exec(user, true, SET_DEVICE_MPROP, "1.2.1.3", "", "test",Json.make("zaza"), null);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "test",null, null);

			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "mac",Json.make("m1"), null);
			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "mac",Json.make("m2"), null);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "mac",null, null);


			result = z.exec(user, true, ADD_LINK_MPROP, "1.2.1.3", "1.2.1.1", "#0.mac",Json.make("m2"), null);
			result = z.exec(user, true, GET_LINK_MPROP, "1.2.1.3", "1.2.1.1", "#0.mac",null, null);


		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		z.saveFiles();
	}


	public static void test1() {

		//DevicesDB z = new DevicesDB();

		DevicesDB2 z=DevicesDB2.instance;

		if (z.isUpdated())
			z.saveFiles();
		z.dump();

		z.saveFiles();

		// enregistrer actions
		// ajouter user
		// record actions (utilsiatble pour roolback, ajouter oldValue)

		z.clear();
		try {
			Json d = z.createDevice(false, "", Json.object().set(IP, "1.1.1.1"));
			Json d2 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.2"));
			Json d3 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.3"));
			for (int i = 4; i < 10; i++)
				z.createDevice(true, "1.1.1." + i, null);

			Json d4 = z.createDevice(false, "", Json.object().set(IP, "1.1.1.67"));

			z.dump();

			z.updateDevice(true, "1.1.1.2", "props.os", Json.make("windows"));
			z.updateDevice(false, d.get(UUID_TAG).asString(), MACS, Json.make("mac1"));
			z.updateDevice(false, d.get(UUID_TAG).asString(), MACS, Json.make("mac2"));
			z.dump();

			z.delDevice(false, d3.get(UUID_TAG).asString());

			z.dump();

			System.out.println("Test");
			System.out.println(z.getDeviceProp(true, "1.1.1.2", "props.os"));
			System.out.println(z.getDeviceProp(true, "1.1.1.1", MACS));
			System.out.println(z.getDeviceProp(true, "1.1.1.1", IP));

			z.addLink(true, "1.1.1.1", "1.1.1.2", Json.object());
			z.dump();

			System.out.println("FIND LINK");
			System.out.println(z.findLink(true, "1.1.1.1", "1.1.1.2"));
			System.out.println(z.findLink(true, "1.1.1.2", "1.1.1.1"));

			Json link = z.findLink(true, "1.1.1.1", "1.1.1.2");

			z.setLinkProps(true, "1.1.1.1", "1.1.1.2", "#0", Json.object().set("test", "essai"));

			for (int i = 0; i < 5; i++) {
				Json p = Json.object();
				p.set("protocol", "TCP");
				p.set("risk", "low");
				p.set("app_protocol", "modbus");
				z.addLinkProps(true, "1.1.1.1", "1.1.1.2", p);
			}
			// z.delLink(true, "1.1.1.1", "1.1.1.2");

			z.dump();

			z.clear();
			Json user = Json.object().set("name", "test");

			z.exec(user, true, ADD_DEVICE, "1.2.1.1", "", "", null, null);
			z.dump();
			System.out.println(z.exec(user, true, GET_DEVICE, "1.2.1.1", "", "", null, null));

			z.exec(user, true, SET_DEVICE_PROP, "1.2.1.1", "", MACS, Json.make("aaaa-bbbb"), null);
			z.exec(user, true, SET_DEVICE_PROP, "1.2.1.1", "", "props.os", Json.make("window"), null);

			Json result = z.exec(user, true, GET_DEVICE_PROP, "1.2.1.1", "", "props.os", null, null);

			System.out.println("os=" + result);

			z.dump();

			// z.exec(user, true,DEL_DEVICE,"1.2.1.1","","",null, null);

			for (int i = 3; i < 10; i++)
				z.exec(user, true, ADD_DEVICE, "1.2.1." + i, "", "", null, null);

			z.exec(user, true, ADD_DEVICE, "1.2.1.2", "", "", null, null);
			z.exec(user, true, ADD_LINK, "1.2.1.2", "1.2.1.1", "", null, null);

			for (int i = 3; i < 5; i++)
				z.exec(user, true, ADD_LINK, "1.2.1." + i, "1.2.1.1", "", null, null);
			for (int i = 3; i <= 5; i++)
				z.exec(user, true, ADD_LINK, "1.2.1." + i, "1.2.1.6", "", null, null);

			System.out.println("\nEND");
			z.dump();

			// z.exec(user, true,DEL_DEVICE,"1.2.1.3","","",null, null);
			z.dump();

			System.out.println(JsonUtil.prettyPrint(z.exec(user, true, GET_DEVICES, "", "", "", null, null)));

			result = z.exec(user, true, SET_DEVICE_PROP, "1.2.1.2", "", "props.os", Json.make("linux"), null);
			System.out.println(result);

			for (int i = 3; i < 5; i++) {
				Json p = Json.object();
				p.set("protocole", "TCP");
				p.set("risk", "low");
				p.set("app_protocol", "modbus");
				// z.addLinkProps(true,"1.2.1."+i, "1.2.1.1", p);
				z.exec(user, true, ADD_LINK_PROPS, "1.2.1." + i, "1.2.1.1", "", p, null);

				p = Json.object();
				p.set("protocole", "UDP");
				p.set("risk", "low");
				p.set("app_protocol", "modbus");
				// z.addLinkProps(true,"1.2.1."+i, "1.2.1.1", p);
				z.exec(user, true, ADD_LINK_PROPS, "1.2.1." + i, "1.2.1.1", "", p, null);

			}

			System.out.println("");
			System.out.println("SET_LINK PROPS");

			result = z.exec(user, true, SET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "0.protocol", Json.make("test_os"), null);
			System.out.println(result);

			result = z.exec(user, true, GET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "#0", Json.make("test_os"), null);
			System.out.println("Elt 0 with id:" + result);

			result = z.exec(user, true, GET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "0", Json.make("test_os"), null);
			System.out.println("Elt 0 with idx:" + result);

			result = z.exec(user, true, GET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "0.protocol", Json.make("test_os"), null);
			System.out.println("prtocol of 0:" + result);
			result = z.exec(user, true, GET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "1._id", Json.make("test_os"), null);
			System.out.println("Id of elt 1:" + result);

			System.out.println("");
			System.out.println("DEL_LINK PROPS");

			// result=z.exec(user, true,DEL_LINK_PROPS,"1.2.1.3", "1.2.1.1","0",null, null);
			// result=z.exec(user, true,GET_LINK,"1.2.1.3", "1.2.1.1","",null, null);

			System.out.println((result));

			result = z.exec(user, true, SET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "0.protocol", Json.make("test_os_linux"),
					null);
			System.out.println(result);

			result = z.exec(user, true, SET_LINK_PROPS, "1.2.1.3", "1.2.1.1", "#0.protocol",
					Json.make("test_os_linux2"), null);
			System.out.println(result);

			result = z.exec(user, true, GET_LINK, "1.2.1.3", "1.2.1.1", "", null, null);
			System.out.println(JsonUtil.prettyPrint(result));

			result = z.exec(user, true, GET_LINKS_FROM, "1.2.1.3", "", "", null, null);
			System.out.println(JsonUtil.prettyPrint(result));
			result = z.exec(user, true, GET_LINKS_TO, "1.2.1.1", "", "", null, null);
			System.out.println(JsonUtil.prettyPrint(result));

			System.out.println("=============");
			System.out.println("=============");
			System.out.println("=============");
			System.out.println("=============");

			// System.out.println(z.exportToJsonTable());

			System.out.println(z.getDevice(true,"1.2.1.3"));


			result = z.exec(user, true, SET_DEVICE_MPROP, "1.2.1.3", "", "test",Json.make("zaza"), null);
			System.out.println(result);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "test",null, null);
			System.out.println(result);

			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "mac",Json.make("m1"), null);
			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "mac",Json.make("m2"), null);
			System.out.println(result);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "mac",null, null);
			System.out.println(result);

			System.out.println(z.getDevice(true,"1.2.1.3"));



			//TESTER ici ajout sur un line

			result = z.exec(user, true, ADD_LINK_MPROP, "1.2.1.3", "1.2.1.1", "#0.mac",Json.make("m2"), null);
			result = z.exec(user, true, GET_LINK_MPROP, "1.2.1.3", "1.2.1.1", "#0.mac",null, null);
			System.out.println(result);


		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		z.saveFiles();

		// System.out.println("List of uuid of devices");
		// System.out.println(z.getListOfUUID());

		// System.out.println(JsonUtil.prettyPrint(z.links));
	}



	public static void test5() {


		DevicesDB2 z=DevicesDB2.instance;

		z.initialized=true;

		z.clear();
		try {


			Json user = Json.object().set("name", "test");

			Json d = z.createDevice(false, "", Json.object().set(IP, "1.2.1.3"));

			System.out.println("dump");
			prettyPrint(z.devices);


			Json result = z.exec(user, true, SET_DEVICE_MPROP, "1.2.1.3", "", "test",Json.make("zaza"), null);

			prettyPrint(z.devices);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "test",null, null);

			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "mac",Json.make("m1"), null);
			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "mac",Json.make("m2"), null);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "mac.1",null, null);
			System.out.println("GET:"+result);


			result = z.exec(user, true, SET_DEVICE_MPROP, "1.2.1.3", "", "mac",Json.make("m3"), null);



			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "mac",null, null);

			System.out.println("GET:"+result);


			//prettyPrint(z.devices);


		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//prettyPrint(z.devices);



		z.saveFiles();



	}


	public static void test6() {


		DevicesDB2 z=DevicesDB2.instance;

		z.initialized=true;

		z.clear();
		try {


			Json user = Json.object().set("name", "test");

			Json d = z.createDevice(false, "", Json.object().set(IP, "1.2.1.3"));

			System.out.println("dump");


			Json result = z.exec(user, true, SET_DEVICE_MPROP, "1.2.1.3", "", "ip",Json.make("1"), null);

			prettyPrint(z.devices);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "ip",null, null);

			System.out.println(result);
			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "macs",Json.make("m1"), null);
			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "macs",Json.make("m2"), null);

			System.out.println("GET:"+result);


			Json jj= Json.object();
			jj.set("test",1);
			jj.set("essai","value");

			//	result = z.exec(user, true, SET_DEVICE_MPROP, "1.2.1.3", "", "xxxx",jj, null);

			z.exec(user, true, SET_DEVICE_PROP, "1.2.1.3", "", MACS, Json.make("aaaa-bbbb"), null);
			z.exec(user, true, SET_DEVICE_PROP, "1.2.1.3", "", "props.os", Json.make("window"), null);

			result = z.exec(user, true, GET_DEVICE_PROP, "1.2.1.3", "", "props.os", null, null);


			result = z.exec(user, true, GET_DEVICE_PROP, "1.2.1.3", "", "macs",null, null);

			//z.exec(user, true, SET_DEVICE_PROP, "1.2.1.3", "", MACS, Json.make("aaaa-bbbb"), null);
			z.exec(user, true, SET_DEVICE_PROP, "1.2.1.3", "", "props.os", Json.make("window"), null);

			result = z.exec(user, true, ADD_DEVICE_MPROP, "1.2.1.3", "", "os",Json.make("linux"), null);
			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "os",Json.make("linux"), null);

			result = z.exec(user, true, SET_DEVICE_MPROP, "1.2.1.3", "", "xxx",Json.object().set("zaz", 1), null);

			System.out.println("__GET:"+result);
			//prettyPrint(z.devices);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "os.size",Json.make("linux"), null);
			System.out.println("__SIZE:"+result);
			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "os[0]",Json.make("linux"), null);
			System.out.println("__value:"+result);
			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "os[1]",Json.make("linux"), null);
			System.out.println("__value:"+result);

			result = z.exec(user, true, GET_DEVICE_MPROP, "1.2.1.3", "", "os[2]",Json.make("linux"), null);
			System.out.println("__value:"+result);





		} catch (DevicesDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//prettyPrint(z.devices);



		z.saveFiles();



	}


	public static void test44() {
		//	String configFile = "";
		//	if (configFile.isEmpty())
		//		configFile = System.getProperty("user.dir") + File.separator + "runconfig/CSLConfigIDS.json";

		DevicesDB.FILE_NAME_DEVICES = "devices_test";

		//	Json j = CSLContext.context.setConfigFileName(configFile);

		// testUpdateModel();


		Json myobject = Json.object();

		Json p=Json.object().set("v",12);
		opOnManagedProp(1, myobject, "x", p, source("manu", "1"));
		System.out.println(prettyPrint(myobject));

		opOnManagedProp(1, myobject, "x", p, source("manu", "1"));
		System.out.println(prettyPrint(myobject));

		;				p.set("v", 13);
		opOnManagedProp(1, myobject, "x", p, source("manu", "1"));
		System.out.println(prettyPrint(myobject));


		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		Json source1 = Json.object().set("type", "manu").set(SOURCE_ID, "121");

		Json source2 = Json.object().set("type", "manu").set(SOURCE_ID, "2");

		opOnManagedProp(2, myobject, "list", ("def"), source2); // ("manu","234"));
		System.out.println(prettyPrint(myobject));


		opOnManagedProp(2, myobject, "list", ("def"), source2); // ("manu","234"));

		System.out.println(prettyPrint(myobject));
		opOnManagedProp(2, myobject, "list", ("abc"), source1);
		opOnManagedProp(3, myobject, "list", ("abc"), source1);
		opOnManagedProp(2, myobject, "list", ("lnm"), source2);

		System.out.println(prettyPrint(myobject));

		System.out.println("idx=" + findIndexOfLastModificationBySource(myobject, "x", source1));

		System.out.println("idx=" + findIndexOfLastModificationBySource(myobject, "list", source1));

		String sobj1 = myobject.toString();

		String sobj = JsonUtil.prettyPrint2(myobject);
		// delAtIndexModificationBySource(myobject, "x", 0);

		// System.out.println(dumpHisto(myobject," "));

		// delAllModificationBySource(myobject, "x", source1);
		// System.out.println("dump histo");
		// System.out.println(dumpHisto(myobject," "));

		// delAllModificationBySource(myobject, "list", source1);

		// System.out.println("dump histo");
		// System.out.println(dumpHisto(myobject, " "));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "x", source("manu", "1"));
		System.out.println("============== undo x manu 1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		System.out.println("============== UNDO list");

		undoLastOpOnManagedProp(myobject, "list", source1);
		System.out.println("============== unod list source1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoLastOpOnManagedProp(myobject, "list", source1);
		System.out.println("============== undo list source1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoAllOpOnManagedProp(myobject, "list", source2);
		System.out.println("============== undo all list source2");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		undoAllOpOnManagedProp(myobject, "list", source1);
		System.out.println("============== undo all list source1");
		// System.out.println(prettyPrint(myobject));

		System.out.println(dumpManagedObject(myobject));

		Json jobj = Json.read(sobj);
		System.out.println(jobj);

		boolean ok = jobj.toString().compareTo(sobj1) == 0;

		System.out.println("Compare:" + ok);

		//setTestModel1();

		//DevicesDB.instance.dump();



	}


	public static void main(String[] args) {

		Json x=Json.make("1");


		System.out.println(x.toString());
		System.out.println(x.asString());

		System.out.println(x.toString().compareTo(x.asString()));

		x=Json.object().set("test", "xxx");
		System.out.println(x.toString());


		DevicesDB.FILE_NAME_DEVICES="devices_test";
		DevicesDB.instance.loadFiles();
		//DevicesDB.instance.dump();


		//test6();
		//DevicesDB.instance.dump();

		//System.out.println("Devices");

		//System.out.println(JsonUtil.prettyPrint1(DevicesDB.instance.devices));
		//DevicesDB.instance.clear();

		//test4();
		//	test3();

		//test1();

		//System.out.println();

		test44();


		Json j=Json.object().set("x",1);
		Json j2=Json.object().set("x",1);

		System.out.println(j.toString());
		//boolean ok=JsonUtil.compare(j1,j2);

		//System.out.println(ok);

		Json j3=Json.read(j.toString());
		System.out.println(j3);

	}
}



