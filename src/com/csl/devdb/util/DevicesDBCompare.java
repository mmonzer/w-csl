package com.csl.devdb.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.csl.core.CSLContext;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

public class DevicesDBCompare {

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
	
	public HashMap<String, String> dictIPtoUUID1 = new HashMap<String, String>();
	public HashMap<String, Json> dictUUIDToDevice1 = new HashMap<String, Json>();

	public HashMap<String, Json> dictUUIDToLink1 = new HashMap<String, Json>();

	
	
	Json devices1 = Json.array();
	Json links1 = Json.array();
	Json networkVues1 = Json.object();

	
	
	public HashMap<String, String> dictIPtoUUID2 = new HashMap<String, String>();
	public HashMap<String, Json> dictUUIDToDevice2 = new HashMap<String, Json>();

	public HashMap<String, Json> dictUUIDToLink2 = new HashMap<String, Json>();

	
	
	Json devices2 = Json.array();
	Json links2 = Json.array();
	Json networkVues2 = Json.object();


	
	
	private Json getLinkFromUUID(String uuid, int n) {
		
		if (n==1)
			return dictUUIDToLink1.get(uuid);
		else
			return dictUUIDToLink2.get(uuid);
	
		
	}
	
	private Json getDeviceFromUUID(String uuid, int n) {
		
		if (n==1)
			return dictUUIDToDevice1.get(uuid);
		else
			return dictUUIDToDevice2.get(uuid);
		
	}
	
	private Json getDeviceFromIP(String ip, int n) {
		
		//System.out.println("  Find device : "+ip+" in "+n );
		
		//dumpIndex(n);
		String uuid= getUUIDFromIP(ip, n);
		
		//System.out.println("  Find device : "+uuid+" in "+n );
		
		
		return getDeviceFromUUID(uuid, n);
		
	}
	
	private String getUUIDFromIP(String uuid, int n) {
		
	
		//System.out.println("  Find device from uuid: "+uuid+" in "+n );
	
		//dumpIndex(n);
		
		String result="";
		
		if (n==1)
			result= dictIPtoUUID1.get(uuid);
		else
			result= dictIPtoUUID2.get(uuid);
		
		
		//System.out.println("    --> result:"+result);
		return result;
	}
	
	private String getIpFromUuid(String uuid, int n) {
		Json d = getDeviceFromUUID(uuid, n);
		
		if (d == null)
			return "#No device";
		String s = d.get(IP).asString();
		if (s == null)
			return "#No ip";
		return s;
	}

	public  String linkToString(Json l, int  n) {
		String src = getIpFromUuid(l.get(SRC_UUID).asString(), n);
		String dst = getIpFromUuid(l.get(DST_UUID).asString(),n);

		return src + "->" + dst + ":" + l;
	}
	
	
	public Json getLinksFrom(String uuid, int n)  {

		
		Json deviceSrc = getDeviceFromUUID(uuid, n);
		if (deviceSrc == null) {
			return null;
		}
		
				

		Json linkSrc = deviceSrc.get(LINKS_FROM);
		if (linkSrc == null) {
			return null;
		}

		Json result = Json.array();
		for (Json luuid : linkSrc.asJsonList()) {
			Json l =getLinkFromUUID(luuid.asString(), n);
			result.add(l);
		}

		return result;

	}
	
	
	public Json getLinksTo(String uuid, int n)  {

		
		Json deviceSrc = getDeviceFromUUID(uuid, n);
		if (deviceSrc == null) {
			return null;
		}
		
				

		Json linkSrc = deviceSrc.get(LINKS_TO);
		if (linkSrc == null) {
			return null;
		}
		
		Json result = Json.array();
		for (Json luuid : linkSrc.asJsonList()) {
			Json l =getLinkFromUUID(luuid.asString(), n);
			// System.out.println(l);
			result.add(l);
		}

		return result;

	}
	
	public Json findLinkBetween( String uuidSrc, int n, String uuidDst) {
	
		Json deviceSrc = getDeviceFromUUID(uuidSrc, n);
		if (deviceSrc == null) {
			System.err.println("Invalid uuid src in find link " + uuidSrc);
			return null;
		}
		
				

		Json linkSrc = deviceSrc.get(LINKS_FROM);
		if (linkSrc == null) return null;
		for (Json luuid : linkSrc.asJsonList()) {
			Json l =getLinkFromUUID(luuid.asString(), n);
			String dst = l.get(DST_UUID).asString();
			if (uuidDst.compareTo(dst) == 0)
					return l;
			}
		
		return null;
	
	}
		
	public Json findLinkBetweenIP( String ipSrc, int n, String ipDst) {
		
		Json deviceSrc = getDeviceFromIP(ipSrc, n);
		if (deviceSrc == null) {
			System.err.println("Invalid ip src in find link " + ipSrc);
			return null;
		}
		
				

		Json linkSrc = deviceSrc.get(LINKS_FROM);
		if (linkSrc == null) return null;
		for (Json luuid : linkSrc.asJsonList()) {
			Json l =getLinkFromUUID(luuid.asString(), n);
			String ipDstfound = getIPDst(l, n); //  l.get(DST_UUID).asString();
			if (ipDst.compareTo(ipDstfound) == 0)
					return l;
			}
		
		return null;
	
	}
	
	private String computeSignature( Json otherProps) {
		String port_src=JsonUtil.getStringFromJson(otherProps, "port_src","-1");
		String port_dst=JsonUtil.getStringFromJson(otherProps, "port_dst","-1");
		String prot=JsonUtil.getStringFromJson(otherProps, "protocol","");
		String app_prot=JsonUtil.getStringFromJson(otherProps, "app_protocol","");
			
		String s=port_src+port_dst+prot+app_prot;
				
		return s;
	}
	
	private Json findSubLink(Json link, Json propsWithSignature) {
		
		if (link==null) return null;
		Json sublinksList = link.get(PROPS_LIST);

		String s1=computeSignature(propsWithSignature);
		for (Json j:sublinksList.asJsonList()) {
			if (computeSignature(j).compareTo(s1)==0) return j;
		}
		return null;
		
	}
	
	private Json getSubLinks(Json link, int n) {
		
		if (link==null) return null;
		return  link.get(PROPS_LIST);
	}
	
	private  String getIPSrc(Json link, int n) {
		
		if (link==null) return null;
		String uuid=  link.get(SRC_UUID).asString();
		return getIpFromUuid(uuid, n);
	}
	
	
	private String getIPDst(Json link, int n) {
		
		if (link==null) return null;
		String uuid=  link.get(DST_UUID).asString();
		return getIpFromUuid(uuid, n);
	}

	private Json buildSignatureProps(int port_src, int port_dst, String protocol, String app_protocol) {
		
		Json props=Json.object();
		
		props.set("port_src", port_src);
		props.set("port_dst", port_dst);
		props.set("protocol", protocol);
		props.set("app_protocol", app_protocol);
		
		
		return props;
	}
	
	/*public Json getLinkFrom(String uuidSrc, int n, String uuidDst, int port_src, int port_dst, String protocol, String app_protocol ) throws DevicesDBException {

		
		Json l= findLinkBetween(uuidSrc, n, uuidDst);
		if (l==null) return null;
		
		return findSubLink(l, buildSignatureProps(port_src, port_dst, protocol, app_protocol));
				
		

	}*/
	
	
	
	public void loadFiles1(String name) {
	
		
		if (CSLContext.instance.getDatabaseServer().docExists(name)) {
			Json j = CSLContext.instance.getDatabaseServer().loadDataFileAsJson(name);
			if (j.has(DEVICES_LIST))
				devices1 = j.get(DEVICES_LIST);
			else {
				System.err.println("Devices not found in doc");
				devices1 = Json.array();
			}
			if (j.has(LINKS_LIST))
				links1 = j.get(LINKS_LIST);
			else {
				System.err.println("Links not found in doc");
				links1 = Json.array();
			}
			if (j.has(NETWORK_VUES_LIST))
				networkVues1 = j.get(NETWORK_VUES_LIST);
			else {
				// System.err.println("Links not found in doc");
				networkVues1 = Json.array();
			}
		} else {
			devices1 = Json.array();
			links1 = Json.array();
			networkVues1 = Json.array();
		}

		for (Json device : devices1.asJsonList()) {
			String uuid = "";
			if (device.has(UUID_TAG))
				uuid = device.get(UUID_TAG).asString();
			else {
				UUID uuidx = UUID.randomUUID();
				uuid = uuidx.toString();
				
				device.set(UUID_TAG, uuid);
			}
			if (!device.has(IP)) {
				System.err.println("Invaild device descriptor (no ip):" + device);
			} else {
				String ip = device.get(IP).asString();
				if (dictIPtoUUID1.get(ip) == null) {
					dictIPtoUUID1.put(ip, uuid);
					dictUUIDToDevice1.put(uuid, device);

				} else {
					System.err.println("Multiple devices with same ip :" + device);
				}
			}

		}

		for (Json link : links1.asJsonList()) {
			String uuid = "";
			if (link.has(UUID_TAG)) {
				uuid = link.get(UUID_TAG).asString();
				dictUUIDToLink1.put(uuid, link);
			} else {
				System.err.println("Invalid link uuid:" + linkToString(link,1));
			}
		}
		
		
		System.out.println("DATABASE1 "+name);
		
		System.out.println(JsonUtil.prettyPrint(devices1));
		System.out.println(JsonUtil.prettyPrint(links1));
		
		System.out.println("Indexes");
		
		for (Map.Entry<String, Json> e : dictUUIDToDevice1.entrySet()) {
			System.out.println(e.getKey()+" ==> "+e.getValue().get(IP));
			}
		for (Map.Entry<String,String> e : dictIPtoUUID1.entrySet()) {
			System.out.println(e.getKey()+" ==> "+e.getValue());
			}

	}
	
	
	public void loadFiles2(String name) {

		
		
		
		
		if (CSLContext.instance.getDatabaseServer().docExists(name)) {
			Json j = CSLContext.instance.getDatabaseServer().loadDataFileAsJson(name);
			if (j.has(DEVICES_LIST))
				devices2 = j.get(DEVICES_LIST);
			else {
				System.err.println("Devices not found in doc");
				devices2 = Json.array();
			}
			if (j.has(LINKS_LIST))
				links2 = j.get(LINKS_LIST);
			else {
				System.err.println("Links not found in doc");
				links2 = Json.array();
			}
			if (j.has(NETWORK_VUES_LIST))
				networkVues2 = j.get(NETWORK_VUES_LIST);
			else {
				// System.err.println("Links not found in doc");
				networkVues2 = Json.array();
			}
		} else {
			devices2 = Json.array();
			links2 = Json.array();
			networkVues2 = Json.array();
		}

		for (Json device : devices2.asJsonList()) {
			String uuid = "";
			if (device.has(UUID_TAG))
				uuid = device.get(UUID_TAG).asString();
			else {
				UUID uuidx = UUID.randomUUID();
				uuid = uuidx.toString();
				
				device.set(UUID_TAG, uuid);
			}
			if (!device.has(IP)) {
				System.err.println("Invaild device descriptor (no ip):" + device);
			} else {
				String ip = device.get(IP).asString();
				if (dictIPtoUUID2.get(ip) == null) {
					dictIPtoUUID2.put(ip, uuid);
					dictUUIDToDevice2.put(uuid, device);

				} else {
					System.err.println("Multiple devices with same ip :" + device);
				}
			}

		}

		for (Json link : links2.asJsonList()) {
			String uuid = "";
			if (link.has(UUID_TAG)) {
				uuid = link.get(UUID_TAG).asString();
				dictUUIDToLink2.put(uuid, link);
			} else {
				System.err.println("Invalid link uuid:" + linkToString(link,2));
			}
		}
		
		
		System.out.println("DATABASE2 "+name);
		System.out.println(JsonUtil.prettyPrint(devices2));
		System.out.println(JsonUtil.prettyPrint(links2));
		
		System.out.println("Indexes");
		
		for (Map.Entry<String, Json> e : dictUUIDToDevice2.entrySet()) {
			System.out.println(e.getKey()+" ==> "+e.getValue().get(IP));
			}
		for (Map.Entry<String,String> e : dictIPtoUUID2.entrySet()) {
			System.out.println(e.getKey()+" ==> "+e.getValue());
			}
	}

	
	
	public void dumpIndex(int n) {
		
		
		if (n==1) {
			
			for (Map.Entry<String, Json> e : dictUUIDToDevice1.entrySet()) {
				System.out.println(e.getKey()+" ==> "+e.getValue().get(IP));
				}
			for (Map.Entry<String,String> e : dictIPtoUUID1.entrySet()) {
				System.out.println(e.getKey()+" ==> "+e.getValue());
				}
			
		}
		else {
			
		
		for (Map.Entry<String, Json> e : dictUUIDToDevice2.entrySet()) {
			System.out.println(e.getKey()+" ==> "+e.getValue().get(IP));
			}
		for (Map.Entry<String,String> e : dictIPtoUUID2.entrySet()) {
			System.out.println(e.getKey()+" ==> "+e.getValue());
			}
		}
	}
	
	public boolean compareDevice(Json device1,int n1, int n2) {
		
		
		
		String ip1=device1.get(IP).asString();
		String uuid1=device1.get(UUID_TAG).asString();
		
		Json device2=getDeviceFromIP(ip1, n2);
		if (device2==null) {
			System.out.println("IP "+ip1+" not found in database "+n2);
			return false;
		}
		
		String ip2=device2.get(IP).asString();
		String uuid2=device1.get(UUID_TAG).asString();
		
		if (ip1.compareTo(ip2)!=0) {
			System.out.println("Difference in IP:\n"+device1+"\n"+device2);
			return false;
		}
		
		Json mac1=device1.get(MACS);
		Json mac2=device2.get(MACS);
		System.out.println("Compare :"+mac1+"  and "+mac2);
		if (!JsonCompare.compare(mac1,  mac2)) {
			System.out.println("Difference in MACS:\n"+device1+"\n"+device2);
			return false;
		}
				
		Json props1=device1.get(PROPS);
		Json props2=device2.get(PROPS);
		if (!JsonCompare.compare(props1, props2)) {
			System.out.println("Difference in PROPS:\n"+device1+"\n"+device2);
			
			return false;
		}
		
				
		// comparer les liens
		
		// pour tous les liens from de d1, noté l1
		//  chercher si un lien egal existe ds d2
		//  get link (ip1, ip2, port_src, port_dst, protocol, app_protocol) ds d2
		//   si oui, comparer les props avec le lien l1
		
		// idem pour les liens to
		
		//String uuidSrc= device1.get(UUID_TAG).asString();
		
		
		for (Json l1:getLinksFrom(uuid1, n1)) {
			String ip_dst=getIPDst(l1, n1);
			Json l2=findLinkBetweenIP(ip2, n2, ip_dst );  // link in database2
			if (l2==null) return false;
			for (Json sublink:getSubLinks(l1,n1)) {
				Json sl2=findSubLink(l2, sublink);   // find the subink in 2
				if (sl2==null) {
					System.out.println("Sublink not found :"+sublink+" in "+ device2);
					return false;
				}
			}
		}
		
		
		for (Json l1:getLinksTo(uuid1, n1)) {
			String ip_src=getIPSrc(l1, n1);
			Json l2=findLinkBetweenIP(ip_src, n2, ip2 );  // link in database2
			if (l2==null) return false;
			for (Json sublink:getSubLinks(l1,1)) {
				Json sl2=findSubLink(l2, sublink);   // find the subink in 2
				if (sl2==null) {
					System.out.println("Sublink not found :"+sublink+" in "+ device2);
					return false;
				}
			}
		}
		
		
		return true;
		

	}
	
	
	public boolean doCompare() {
		
		System.out.println("Check 1 in 2");
		for (Json device:devices1) {
			System.out.println("Device in base 1: "+device.get(IP));
			boolean ok =compareDevice(device, 1,2);
			if (!ok) return false;
		}
		
		System.out.println("Check 2 in 1");
		for (Json device:devices2) {
			System.out.println("Device in base 2: "+device.get(IP));
			
			boolean ok =compareDevice(device, 2,1);
			if (!ok) return false;
		}
		
		return true;
	}
	
	public Json compareDataBase(String p1, String p2) {
		// TODO Auto-generated method stub
		loadFiles1(p1);
		loadFiles2(p2);
		
		
		Json result= Json.object();
		boolean b= doCompare();
		result.set("value",b);
		return result;
	}
	

	
	
}
