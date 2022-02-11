package main.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mitre.cpe.common.WellFormedName;
import org.mitre.cpe.matching.CPENameMatcher;
import org.mitre.cpe.naming.CPENameUnbinder;

import com.xcsl.json.Json;

public class Test01NVD {



	private static Json readDataFile(String path) 
	{



		String content = "";

		try
		{
			content = new String ( Files.readAllBytes( Paths.get(path) ) );
			Json z=Json.read(content);
			return z;
		} 
		catch (IOException e) 
		{
			//e.printStackTrace();
			//return "{\"Error\":\"File not found:"+e.getMessage()+"\"}";
			Json z=Json.object();
			z.set("contents",Json.object());
			z.set("error","Object not found ("+e.getMessage()+")");

			return z;

		}

		//return content;
	}


	static CPENameUnbinder cpenu = new CPENameUnbinder();
	static     CPENameMatcher cpenm = new CPENameMatcher();


	public static boolean contains(String cpeUri,WellFormedName key) {


		WellFormedName wfn;
		try {
			wfn = cpenu.unbindFS(cpeUri);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.err.println("Invalid cpemask:"+cpeUri);
			return false;
			//	e.printStackTrace();
		}

		boolean b=(cpenm.isSubset(wfn, key)); // true, key is a subset of wfn

		if (b) System.out.println(cpeUri +" contains" +key);
		return b;
	}

	public static boolean eval(Json term, WellFormedName key ) {
		String s="";
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
		else
			return true; // default, should not occur
	}



	static Json all= Json.array();


	public static void load(String path) {

		System.out.println("Loading cve database");
		for (int i=2002; i<2022; i++ ) {
		//for (int i=2020; i<2021; i++ ) {

			System.out.println("  year "+i);
			Json jf=readDataFile(path+"nvdcve-1.1-"+i+".json");


			Json items = jf.get("CVE_Items");
			for (Json ii:items.asJsonList()) {
				all.add(ii);
				//System.out.println(ii);
			}
			//	System.out.println(jf);
		}
		System.out.println("End of loading");

	}
	
	static List<CVEDescriptor> allcve= new ArrayList<CVEDescriptor>();
	
	public static void load2(String path) {

		System.out.println("Loading cve database");
		for (int i=2002; i<2022; i++ ) {
		//for (int i=2020; i<2021; i++ ) {

			System.out.println("  year "+i);
			Json jf=readDataFile(path+"nvdcve-1.1-"+i+".json");


			Json items = jf.get("CVE_Items");
			for (Json ii:items.asJsonList()) {
				allcve.add(new CVEDescriptor().initFromCveJson(ii));
				//System.out.println(ii);
			}
			//	System.out.println(jf);
		}
		System.out.println("End of loading");

	}

	public static String prettyPrint(Json term, String decal ) {
		String s="";
		Json op=term.get("operator");

		if (op==null) {
			return decal+term.get("cpe23Uri");
		}
		String sop=op.asString();

		if (sop.compareTo("OR")==0) {
			s=s+decal+"OR\n";
			for (Json x:term.get("cpe_match").asJsonList()) {
				s=s+prettyPrint(x, decal+"   ")+"\n";
			}	
			return s;
		}

		else if (sop.compareTo("AND")==0) {
			s=s+decal+"AND\n";
			Json termsList=term.get("children");
			if (termsList==null) {
				termsList=term.get("cpe_match");
			}
			for (Json x:termsList.asJsonList()) {
				s=s+prettyPrint(x, decal+"   ");
			}

			return s;
		}
		else
			return decal+term.toString();
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
		else
			return decal+term.toString();
	}
	
	public static String normalize(String s) {
		return s.replaceAll("\\s+", "_")
	             .replaceAll("[^_a-zA-Z0-9]", "");
	}
	public static List<String> getAllCpe(Json items) {



		CPENameUnbinder cpenu = new CPENameUnbinder();

		//List<String> names= new ArrayList<String>();

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

		//System.out.println(term);
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
	
	
	static public String writeFile(String filename, String content) {

		try {

			Files.write(Paths.get(filename), content.getBytes());
			return "ok";
		} catch (IOException e) {
			e.printStackTrace();
			return "error "+e.getMessage();
		}

	}
	
	
	
	
	public static void main(String[] args) {

		
		long heapFreeSize = Runtime.getRuntime().freeMemory(); 
		System.out.println(heapFreeSize);
		
		CPENameUnbinder cpenu = new CPENameUnbinder();
		try {
			//WellFormedName key= cpenu.unbindURI("cpe:/o:linux:linux_kernel:2.6.32");
			WellFormedName key= cpenu.unbindURI("cpe:/o:linux:linux_kernel:2.6");


			//WellFormedName key= cpenu.unbindURI("cpe:/a:python:python:3.7.3");

			//String key="siemens";
			String path="/Users/flausj/Documents/usb/tempo/nvd/";
			//Json jf=readDataFile("/Users/flausj/Downloads/nvdcve-1.1-2021.json");

			//System.out.println(JsonUtil.prettyPrint(jf));0

			//		System.out.println(jf);
			load2(path);
			Json items = all; //get("CVE_Items");
//			int n=0,c=0,found =0;
//			for (Json jj:items.asJsonList()) {
//				//System.out.println(jj.get("configurations").get("nodes"));
//				if (c>=1000) {
//					System.out.println("Scanned "+n+" vulnerabilities");
//					c=0;
//				}
//				n++;c++;
//
//				Json x=jj.get("configurations").get("nodes");
//				for (Json term:x.asJsonList()) {
//					//System.out.println(prettyPrint(term, ""));
//					if (eval(term,key)) {
//						System.err.println("==>> FOUND ");
//						System.err.println(prettyPrint(term, ""));
//						found++;
//					}
//				}
//			}

			//getAllCpe(all);

			
			String s="";
			
//			FileOutputStream fos = new FileOutputStream(path+"test.txt", false);
//			 //fos.write("[\n".getBytes());
//			    
//			for (Json jj:items.asJsonList()) {
//				CVEDescriptor cveDescriptor = new CVEDescriptor();
//				cveDescriptor.initFromCveJson(jj);
//				//System.out.println(JsonUtil.prettyPrint(cveDescriptor.toJson()));
//				//if (!s.isEmpty()) s=",\n";
//				s=cveDescriptor.toJson().toString();
//				 fos.write(s.getBytes());
//			}
//			
//			
//			 //fos.write("]\n".getBytes());
//			fos.close();
			//System.out.println("Found "+found+" vulnerabilities");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
//		catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		long heapFreeSizef = Runtime.getRuntime().freeMemory(); 
		
		System.out.println(heapFreeSize);
		System.out.println(heapFreeSizef);
		System.out.println(heapFreeSizef-heapFreeSize);
		
		long size=0;
		for (CVEDescriptor c:allcve) {
			size=size+c.getSize();
		}
		
		size=size/1024;
		size=size/1024;
		
		System.out.println("Size:"+size);
		//JsonUtil.readFileAsJson(f)
		
		List<WellFormedName> keys= new ArrayList<WellFormedName>();
		
		WellFormedName key2;
		try {
			key2 = cpenu.unbindURI("cpe:/o:linux:linux_kernel:2.6");
			keys.add(key2);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		int n=0,c=0, found=0;
		for (CVEDescriptor cve:allcve) {
//		//System.out.println(jj.get("configurations").get("nodes"));
		if (c>=1000) {
			System.out.println("Scanned "+n+" vulnerabilities");
			c=0;
		}
		n++;c++;

	
		
//		Json x=jj.get("configurations").get("nodes");
//		for (Json term:x.asJsonList()) {
//			//System.out.println(prettyPrint(term, ""));
			if (cve.eval(keys)) {
				System.err.println("==>> FOUND ");
				System.err.println(prettyPrint(cve.toJson(), ""));
				found++;
			}
		}
//	}

	}





}
