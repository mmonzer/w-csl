package main.extensions;

import com.csl.core.CSLContext;
import com.ucsl.json.Json;
import lib.unpacked.org.mitre.cpe.common.WellFormedName;
import lib.unpacked.org.mitre.cpe.matching.CPENameMatcher;
import lib.unpacked.org.mitre.cpe.naming.CPENameUnbinder;
import main.demo.CVEDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class CveSearch {
	static CPENameUnbinder cpenu = new CPENameUnbinder();
	static CPENameMatcher cpenm = new CPENameMatcher();
	static Json all= Json.array();
	static List<CVEDescriptor> allcve= new ArrayList<CVEDescriptor>();
	
	private static Json readDataFile(String path) {
		String content = "";
		try{
			content = new String ( Files.readAllBytes( Paths.get(path) ) );
			Json z=Json.read(content);
			return z;
		} 
		catch (IOException e) {
			Json z=Json.object();
			z.set("contents",Json.object());
			z.set("error","Object not found ("+e.getMessage()+")");
			return z;
		}
	}

	public static boolean contains(String cpeUri,WellFormedName key) {
		WellFormedName wfn;
		try {
			wfn = cpenu.unbindFS(cpeUri);
		} catch (ParseException e) {
			return false;
		}

		boolean b=(cpenm.isSubset(wfn, key)); // true, key is a subset of wfn

		if (b) System.out.println(cpeUri +" contains" +key);
		return b;
	}

	public static boolean eval(Json term, WellFormedName key ) {
		System.out.println(term.toString());
		Json op=term.get("operator");
		if (op==null) {
			return contains(term.get("cpe23Uri").asString(), key);
		}
		String sop=op.asString();

		if (sop.compareTo("OR")==0) {

			for (Json x:term.get("cpe_match").asJsonList()) {
				if (eval(x, key)) return true;
			}	
			return false;
		}

		else if (sop.compareTo("AND")==0) {

			Json termsList=term.get("children");
			if (termsList==null) {
				termsList=term.get("cpe_match");
			}
			for (Json x:termsList.asJsonList()) {
				if (!eval(x, key)) return false;
			}	
			return true;
		}
		else {
			return true; // default, should not occur
		}
	}

	public static void load2(String path) {
		System.out.println("Loading cve database");
		for (int i=2002; i<2022; i++ ) {
			System.out.println("loaded year "+i);
			Json jf=readDataFile(path+"nvdcve-1.1-"+i+".json");
			Json items = jf.get("CVE_Items");
			for (Json ii:items.asJsonList()) {
				allcve.add(new CVEDescriptor().initFromCveJson(ii));
			}
		}
		System.out.println("End of loading");
	}
	
	public static void load3(String path, ArrayList<WellFormedName> keys) {
		System.out.println("Loading cve database");
		Json jf, items;
		CVEDescriptor tmp;
		for (int i=2002; i<2022; i++ ) {
			System.out.println("loaded year "+i);
			jf=readDataFile(path+"nvdcve-1.1-"+i+".json");
			items = jf.get("CVE_Items");
			for (Json ii:items.asJsonList()) {
				tmp = new CVEDescriptor().initFromCveJson(ii);
				try {
					if(tmp.eval(keys))
						allcve.add(tmp);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("End of loading");
	}
	public static String toExp(Json term, String decal ) {
		String s="";
		Json op=term.get("operator");
		if (op==null) {
			String sx=term.get("cpe23Uri").toString();
			if (sx.contains(" ")) System.err.println("Error char & in :"+sx);
			return "CPE "+sx;
		}
		String sop=op.asString();
		if (sop.compareTo("OR")==0) {
			s=s+"OR_START ";
			for (Json x:term.get("cpe_match").asJsonList()) {
				if (!s.endsWith(" ")) s=s+" ";
				s=s+toExp(x, decal);
				
			}	
			if (!s.endsWith(" ")) s=s+" ";
			s=s+"OR ";
			return s;
		}
		else if (sop.compareTo("AND")==0) {
			s=s+"AND_START ";
			Json termsList=term.get("children");
			if (termsList==null) {
				termsList=term.get("cpe_match");
			}
			for (Json x:termsList.asJsonList()) {
				s=s+toExp(x, decal);
				if (!s.endsWith(" ")) s=s+" ";
			}
			if (!s.endsWith(" ")) s=s+" ";
			s=s+"AND ";
			return s;
		}
		else {
			return decal+term.toString();
		}
	}
	
	public static String normalize(String s) {
		return s.replaceAll("\\s+", "_")
	             .replaceAll("[^_a-zA-Z0-9]", "");
	}
	
	public static List<String> getAllCpe(Json items) {
		CPENameUnbinder cpenu = new CPENameUnbinder();
		Map<String, Integer> vendors = new HashMap<String, Integer>();
		for (Json item:items.asJsonList()) {
			Json cve=item.get("configurations").get("nodes");
			//System.out.println(cve);
			for (Json term:cve.asJsonList()) {
				List<String> listCpe = getCpeUris(term, null);

				for (String scpe:listCpe) {
					try {
						WellFormedName wfn3 = cpenu.unbindFS(scpe);
						String vendor="";
						if (wfn3.get("vendor") instanceof String) {
							vendor=(String)wfn3.get("vendor");
						}
						else {
							System.err.println("Invalid vendor:"+scpe);
							System.err.println("Invalid vendor:"+wfn3.get("vendor"));
							vendor="???";
						}
						vendor=normalize(vendor);
						//System.out.println(vendor);
						if (vendors.get(vendor)==null) 
							vendors.put(vendor, 1);
						else
							vendors.put(vendor, vendors.get(vendor)+1);

					} catch (ParseException e) {
						// TODO Auto-generated catch block
						System.out.println("Invalid CPE:"+scpe);
						e.printStackTrace();
					}

				}
			}
		}
		List<String> names= new ArrayList<String>(vendors.keySet());
		Collections.sort(names);
		
		for (String n:names) {
			System.out.println(n + "  : " + vendors.get(n));
			}
		System.out.println("Number of vendors:"+names.size());
		return names;

	}

	public static List<String> getCpeUris(Json term, List<String> cpeUris) {

		if (cpeUris==null) cpeUris= new ArrayList<String>();
			Json op=term.get("operator");

		if (op==null) {
			String s=term.get("cpe23Uri").asString();
			cpeUris.add(s);
			return cpeUris;
		}
		String sop=op.asString();
		if (sop.compareTo("OR")==0) {
			for (Json x:term.get("cpe_match").asJsonList()) {

				getCpeUris(x, cpeUris);
			}	
			return cpeUris;
		}

		else if (sop.compareTo("AND")==0) {
			Json termsList=term.get("children");
			if (termsList==null) {
				termsList=term.get("cpe_match");
			}
			for (Json x:termsList.asJsonList()) {
				getCpeUris(x, cpeUris);
			}
			return cpeUris;
		}
		else
			return cpeUris;
	}
	
	public ArrayList<Json> getFullCve(String cpe) throws ParseException{
		List<WellFormedName> keys= new ArrayList<WellFormedName>();
		WellFormedName key2 = cpenu.unbindURI(cpe);
		keys.add(key2);
		ArrayList<Json> result = new ArrayList<Json>();
		for (CVEDescriptor cve:allcve) {
			try {
				if (cve.eval(keys)) {
					result.add(cve.toJson());
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	String bddIp,bddPort;


	public CveSearch(ArrayList<String> keysString) throws ParseException {
		String path=   CSLContext.instance.getCslConfDir()+"/cve/data/";// getIdsParams().getDataDir()+"/cve/data/";
		System.out.println("path : "+path);
		ArrayList<WellFormedName> keys = new ArrayList<WellFormedName>();
		for(String s : keysString) {
			WellFormedName w = cpenu.unbindURI(s);
			keys.add(w);
		}
		load3(path, keys);
	}
}