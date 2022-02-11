package main.extensions;

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

import com.csl.core.CSLContext;
import com.xcsl.json.Json;

import main.demo.CVEDescriptor;

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
	
	public static void main(String[] args) {

		CPENameUnbinder cpenu = new CPENameUnbinder();
		List<WellFormedName> keys= new ArrayList<WellFormedName>();
		WellFormedName key2;
		
		try {
			String path="./datafile/cve/";
			load2(path);
			key2 = cpenu.unbindURI("cpe:/o:linux:linux_kernel:2.6");
			keys.add(key2);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		int found=0;
		for (CVEDescriptor cve:allcve) {
			if (cve.eval(keys)) {
				System.out.println(cve.toJson());
				found++;
			}
		}
		System.out.println("found "+found+" cve");

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
	/*private String formatCpe(String cpe) {
		cpe = cpe.trim();
		if(!cpe.startsWith("cpe:2.3:")) {
			if(!cpe.startsWith("cpe:/")) {
				return cpe;
			}
			cpe=cpe.replaceFirst("cpe:/","cpe:2.3:");
			cpe=cpe.replaceFirst("::",":-:");
			cpe=cpe.replaceFirst("~-","~");
			cpe=cpe.replaceFirst("~",":-:");
			cpe=cpe.replaceFirst("::",":");
			StringUtils.strip(cpe,"':-'");
		}
		return cpe;
	}
	
	public ArrayList<Json> findCompleteCveForCpe(String cpe) {
		
		ArrayList<Json> result = new ArrayList<Json>();
		cpe = formatCpe(cpe);
		System.out.println("used cpe :"+cpe);
		MongoClient mongoClient = new MongoClient(bddIp , Integer.parseInt(bddPort));
		MongoDatabase database = mongoClient.getDatabase("cvedb");
		MongoCollection<Document> collection = database.getCollection("cves");
		BasicDBObject query = new BasicDBObject("vulnerable_configuration", new BasicDBObject("$regex", cpe+"(?:\\:\\*|\\:\\-)(?:\\:\\*)+").append("$options", "i"));
		MongoCursor<Document> cursor = collection.find(query).iterator();
		while(cursor.hasNext()) {
			Json j = Json.read(cursor.next().toJson());
			j.delAt("vulnerable_configuration");
			j.delAt("vulnerable_configuration_cpe_2_2");
			j.delAt("vulnerable_product");
			j.delAt("references");
			result.add(j);			
		}
		
		
		mongoClient.close();
		return result;
	}
	
	
	
	
	
	
    private Json getCveForCPE(String cpe, boolean cvssTest, double cvssScore) throws Exception {
        String url = "http://"+bddIp+":"+bddPort+"/api/cvefor/"+cpe;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
       //optional default is GET
        con.setRequestMethod("GET");
       //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        System.out.println("url:"+url);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
    	Pattern patternId = Pattern.compile("^.*\"id\": \"CVE.*", Pattern.DOTALL);
    	Pattern patternCvss = Pattern.compile("^.*\"cvss\":.*", Pattern.DOTALL);
    	ArrayList<String> ids = new ArrayList<String>();
    	ArrayList<String> cvss = new ArrayList<String>();
        while ((inputLine = in.readLine()) != null) {  
        	Matcher matcher = patternId.matcher(inputLine.toString());
        	if(matcher.matches()) {
        		String id = inputLine.split(":")[1];
        		id = id.substring(id.indexOf("\"")+1, id.lastIndexOf("\""));
        		ids.add(id);
        	}
        	matcher = patternCvss.matcher(inputLine.toString());
        	if(matcher.matches()) {
        		String test;
        		test = inputLine.substring(inputLine.lastIndexOf(":")+1, inputLine.lastIndexOf(","));
        		cvss.add(test);
        	}        	
        }
        in.close();
    	if(cvssTest && (ids.size() == cvss.size())){
        	ArrayList<String> idsFinal = new ArrayList<String>();
        	for(int i = 0; i < ids.size(); i++) {
        		if(Double.parseDouble(cvss.get(i)) >= cvssScore) {
        			idsFinal.add(ids.get(i));
        		}
        	}   		
        	ids=idsFinal;
    	}
    	else if(cvssTest && !(ids.size() == cvss.size())){
    		System.out.println("CVSS test impossible, returning all CVE ..");
    	}
        Json resultJson = Json.object();
        resultJson.at("cpe",cpe);
        resultJson.at("cve",ids);
        return resultJson;
    }*/
    
    /**
     * Récupère les ID des CVE associées à la machine décrite au format CPE passée en argument.
     * @param cpe La description format CPE de la machine
     * @return Un object JSON contenant un tableau dans son champ "result". 
     * Ce tableau est constitués d'objets Json contenant deux champs :
     *     -cpe : Le CPE demandé
     *     -cve : Un tableau Json contenant les ID des CVE 
     * Exemple : {"result":[{"cve":["CVE-2009-1298","CVE-2009-3547"],"cpe":"cpe:/o:linux:linux_kernel:2.6.32"}]}
     */
    /*public Json getCVE(String cpe) {
    	Json result = Json.object();
    	ArrayList<Json> resultTab = new ArrayList<Json>();
    	try {
			resultTab.add(getCveForCPE(cpe, false, 0));
		} catch (Exception e) {
			e.printStackTrace();
		}
		result.at("result",resultTab);
    	return result;
    }*/
    
    /**
     * Récupère les ID des CVE associées à la machine décrite au format CPE passée en argument.
     * @param cpe La description format CPE de la machine
     * @param cvssMin Le score CVSS minimal pour les CVE retournées
     * @return Un object JSON contenant un tableau dans son champ "result". 
     * Ce tableau est constitués d'objets Json contenant deux champs :
     *     -cpe : Le CPE demandé
     *     -cve : Un tableau Json contenant les ID des CVE 
     * Exemple : {"result":[{"cve":["CVE-2009-1298","CVE-2009-3547"],"cpe":"cpe:/o:linux:linux_kernel:2.6.32"}]}
     */
    /*public Json getCVE(String cpe, double cvssMin) {
    	Json result = Json.object();
    	ArrayList<Json> resultTab = new ArrayList<Json>();
    	try {
			resultTab.add(getCveForCPE(cpe, true, cvssMin));
		} catch (Exception e) {
			e.printStackTrace();
		}
		result.at("result",resultTab);
    	return result;
    } */ 
    
    /**
     * Récupère les ID des CVE associées à chacune des machines décrites au format CPE dans la liste passée en argument. 
     * @param cpe Un tableau contenant les différentes descriptions au format CPE des machines à tester
     * @return Un object JSON contenant un tableau dans son champ "result". 
     * Ce tableau est constitués d'objets Json contenant deux champs :
     *     -cpe : Le CPE demandé
     *     -cve : Un tableau Json contenant les ID des CVE 
     * Exemple : {"result":[{"cve":["CVE-2009-1298","CVE-2009-3547"],"cpe":"cpe:/o:linux:linux_kernel:2.6.32"},{"cve":["CVE-2009-1298","CVE-2009-3547"],"cpe":"cpe:/o:linux:linux_kernel:2.6.32"}]}
     */
    /*public Json getCVE(ArrayList<String> cpeTab) {
    	Json result = Json.object();
    	ArrayList<Json> resultTab = new ArrayList<Json>();
    	for(String cpe : cpeTab) {
	    	try {
				resultTab.add(getCveForCPE(cpe, false, 0));
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
		result.at("result",resultTab);
    	return result;
    }	*/
    
    /**
     * Récupère les ID des CVE associées à chacune des machines décrites au format CPE dans la liste passée en argument. 
     * @param cpeTabcpe Un tableau contenant les différentes descriptions au format CPE des machines à tester
     * @param cvssMin Le score minimal CVSS pour qu'une CVE soit ajoutée
     * @return Un object JSON contenant un tableau dans son champ "result". 
     * Ce tableau est constitués d'objets Json contenant deux champs :
     *     -cpe : Le CPE demandé
     *     -cve : Un tableau Json contenant les ID des CVE 
     * Exemple : {"result":[{"cve":["CVE-2009-1298","CVE-2009-3547"],"cpe":"cpe:/o:linux:linux_kernel:2.6.32"},{"cve":["CVE-2009-1298","CVE-2009-3547"],"cpe":"cpe:/o:linux:linux_kernel:2.6.32"}]}
     */
    /*public Json getCVE(ArrayList<String> cpeTab, double cvssMin) {
    	Json result = Json.object();
    	ArrayList<Json> resultTab = new ArrayList<Json>();
    	for(String cpe : cpeTab) {
	    	try {
				resultTab.add(getCveForCPE(cpe, true, cvssMin));
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
		result.at("result",resultTab);
    	return result;
    }   
    
    
    
    private Json getCveDetails(String cve) throws Exception {
        String url = "http://"+bddIp+":"+bddPort+"/api/cve/"+cve;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
       //optional default is GET
        con.setRequestMethod("GET");
       //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        
        Json j = Json.object();
        j = Json.read(response.toString());
        j.atDel("vulnerable_configuration");
        j.atDel("vulnerable_product");
        j.atDel("vulnerable_configuration_cpe_2_2");
        return j;
    }*/
    
    /**
     * Récupère les informations pour une CVE d'ID donné en argument
     * @param cve L'ID de la CVE dont on veut récupèrer les informations
     * @return Un object JSON dont le champ "result" contient un tableau JSON d'object JSON 
     * Chacun de ces objets JSON décrit une CVE et contient les champs suivants :
     *     -summary
     *     -Modified
     *     -access
     *     -references
     *     -impact
     *     -assigner
     *     -statements
     *     -refmap
     *     -capec
     *     -cwe
     *     -Published
     *     -id
     *     -cvss-time
     *     -cvss
     *     -cvss-vector
     * Voir sur internet pour voir à quoi correspondent ces champs.  
     * Exemple :  {"result":[{"summary":"The ip_frag_reasm function in net/ipv4/ip_fragment.c in the Linux kernel 2.6.32-rc8, and 2.6.29 and later versions before 2.6.32, calls IP_INC_STATS_BH with an incorrect argument, which allows remote attackers to cause a denial of service (NULL pointer dereference and hang) via long IP packets, possibly related to the ip_defrag function.","Modified":"2018-10-10T19:35:00","access":{"complexity":"LOW","vector":"NETWORK","authentication":"NONE"},"references":["http://git.kernel.org/?p=linux/kernel/git/torvalds/linux-2.6.git;a=commitdiff;h=bbf31bf18d34caa87dd01f08bf713635593697f2","http://lists.opensuse.org/opensuse-security-announce/2010-01/msg00000.html","http://secunia.com/advisories/37624","http://secunia.com/advisories/38017","http://twitter.com/spendergrsec/statuses/6339560349","http://wiki.rpath.com/Advisories:rPSA-2009-0161","http://www.kernel.org/pub/linux/kernel/v2.6/ChangeLog-2.6.32","http://www.mandriva.com/security/advisories?name=MDVSA-2009:329","http://www.osvdb.org/60788","http://www.securityfocus.com/archive/1/508517/100/0/threaded","http://www.theregister.co.uk/2009/12/11/linux_kernel_bugs_patched/","http://www.ubuntu.com/usn/USN-869-1","https://bugzilla.redhat.com/show_bug.cgi?id=544144","https://www.redhat.com/archives/fedora-package-announce/2009-December/msg00453.html","https://www.redhat.com/archives/fedora-package-announce/2009-December/msg00496.html"],"impact":{"integrity":"NONE","confidentiality":"NONE","availability":"COMPLETE"},"assigner":"cve@mitre.org","statements":[{"contributor":"Mark J Cox","lastmodified":"2009-12-09","organization":"Red Hat","statement":"Not vulnerable. This issue did not affect the versions of the Linux kernel as shipped with Red Hat Enterprise Linux 3, 4, 5 and Red Hat Enterprise MRG as they did not include upstream commit 7c73a6fa that introduced the problem."}],"refmap":{"confirm":["http://git.kernel.org/?p=linux/kernel/git/torvalds/linux-2.6.git;a=commitdiff;h=bbf31bf18d34caa87dd01f08bf713635593697f2","http://wiki.rpath.com/Advisories:rPSA-2009-0161","http://www.kernel.org/pub/linux/kernel/v2.6/ChangeLog-2.6.32","https://bugzilla.redhat.com/show_bug.cgi?id=544144"],"osvdb":["60788"],"secunia":["37624","38017"],"suse":["SUSE-SA:2010:001"],"mandriva":["MDVSA-2009:329"],"fedora":["FEDORA-2009-12786","FEDORA-2009-12825"],"ubuntu":["USN-869-1"],"bugtraq":["20091216 rPSA-2009-0161-1 hwdata kernel"],"misc":["http://twitter.com/spendergrsec/statuses/6339560349","http://www.theregister.co.uk/2009/12/11/linux_kernel_bugs_patched/"]},"capec":[{"prerequisites":"The application uses environment variables. An environment variable exposed to the user is vulnerable to a buffer overflow. The vulnerable environment variable uses untrusted data. Tainted data used in the environment variables is not properly validated. For instance boundary checking is not done before copying the input data to a buffer.","summary":"This attack pattern involves causing a buffer overflow through manipulation of environment variables. Once the attacker finds that they can modify an environment variable, they may try to overflow associated buffers. This attack leverages implicit trust often placed in environment variables.","solutions":"Do not expose environment variable to the user. Do not use untrusted data in your environment variables. Use a language or compiler that performs automatic bounds checking There are tools such as Sharefuzz [R.10.3] which is an environment variable fuzzer for Unix that support loading a shared library. You can use Sharefuzz to determine if you are exposing an environment variable vulnerable to buffer overflow.","name":"Buffer Overflow via Environment Variables","related_weakness":["118","119","120","20","302","680","697","733","74","99"],"id":"10"},{"prerequisites":"Targeted software performs buffer operations. Targeted software inadequately performs bounds-checking on buffer operations. Adversary has the capability to influence the input to buffer operations.","summary":"Buffer Overflow attacks target improper or missing bounds checking on buffer operations, typically triggered by input injected by an adversary. As a consequence, an adversary is able to write past the boundaries of allocated buffer regions in memory, causing a program crash or potentially redirection of execution as per the adversaries' choice.","solutions":"Use a language or compiler that performs automatic bounds checking. Use secure functions not vulnerable to buffer overflow. If you have to use dangerous functions, make sure that you do boundary checking. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Use OS-level preventative functionality. Not a complete solution. Utilize static source code analysis tools to identify potential buffer overflow weaknesses in the software.","name":"Overflow Buffers","related_weakness":["119","120","129","131","19","680","805"],"id":"100"},{"prerequisites":"The adversary must identify a programmatic means for interacting with a buffer, such as vulnerable C code, and be able to provide input to this interaction.","summary":"An adversary manipulates an application's interaction with a buffer in an attempt to read or modify data they shouldn't have access to. Buffer attacks are distinguished in that it is the buffer space itself that is the target of the attack rather than any code responsible for interpreting the content of the buffer. In virtually all buffer attacks the content that is placed in the buffer is immaterial. Instead, most buffer attacks involve retrieving or providing more input than can be stored in the allocated buffer, resulting in the reading or overwriting of other unintended program memory.","solutions":"To help protect an application from buffer manipulation attacks, a number of potential mitigations can be leveraged. Before starting the development of the application, consider using a code language (e.g., Java) or compiler that limits the ability of developers to act beyond the bounds of a buffer. If the chosen language is susceptible to buffer related issues (e.g., C) then consider using secure functions instead of those vulnerable to buffer manipulations. If a potentially dangerous function must be used, make sure that proper boundary checking is performed. Additionally, there are often a number of compiler-based mechanisms (e.g., StackGuard, ProPolice and the Microsoft Visual Studio /GS flag) that can help identify and protect against potential buffer issues. Finally, there may be operating system level preventative functionality that can be applied.","name":"Buffer Manipulation","related_weakness":["119"],"id":"123"},{"prerequisites":"The targeted client software communicates with an external server. The targeted client software has a buffer overflow vulnerability.","summary":"This type of attack exploits a buffer overflow vulnerability in targeted client software through injection of malicious content from a custom-built hostile service.","solutions":"The client software should not install untrusted code from a non-authenticated server. The client software should have the latest patches and should be audited for vulnerabilities before being used to communicate with potentially hostile servers. Perform input validation for length of buffer inputs. Use a language or compiler that performs automatic bounds checking. Use an abstraction library to abstract away risky APIs. Not a complete solution. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Ensure all buffer uses are consistently bounds-checked. Use OS-level preventative functionality. Not a complete solution.","name":"Client-side Injection-induced Buffer Overflow","related_weakness":["118","119","120","20","353","680","697","713","74"],"id":"14"},{"prerequisites":"Ability to control the length of data passed to an active filter.","summary":"In this attack, the idea is to cause an active filter to fail by causing an oversized transaction. An attacker may try to feed overly long input strings to the program in an attempt to overwhelm the filter (by causing a buffer overflow) and hoping that the filter does not fail securely (i.e. the user input is let into the system unfiltered).","solutions":"Make sure that ANY failure occurring in the filtering or input validation routine is properly handled and that offending input is NOT allowed to go through. Basically make sure that the vault is closed when failure occurs. Pre-design: Use a language or compiler that performs automatic bounds checking. Pre-design through Build: Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Operational: Use OS-level preventative functionality. Not a complete solution. Design: Use an abstraction library to abstract away risky APIs. Not a complete solution.","name":"Filter Failure through Buffer Overflow","related_weakness":["118","119","120","20","680","697","733","74"],"id":"24"},{"prerequisites":"The target system uses a mail server. Mail server vendor has not released a patch for the MIME conversion routine, the patch itself has a security hole or does not fix the original problem, or the patch has not been applied to the user's system.","summary":"An attacker exploits a weakness in the MIME conversion routine to cause a buffer overflow and gain control over the mail server machine. The MIME system is designed to allow various different information formats to be interpreted and sent via e-mail. Attack points exist when data are converted to MIME compatible format and back.","solutions":"Stay up to date with third party vendor patches From \"Exploiting Software\", please see reference below. Use the sendmail restricted shell program (smrsh) Use mail.local","name":"MIME Conversion","related_weakness":["119","120","20","74"],"id":"42"},{"prerequisites":"Target software processes binary resource files. Target software contains a buffer overflow vulnerability reachable through input from a user-controllable binary resource file.","summary":"An attack of this type exploits a buffer overflow vulnerability in the handling of binary resources. Binary resources may include music files like MP3, image files like JPEG files, and any other binary file. These attacks may pass unnoticed to the client machine through normal usage of files, such as a browser loading a seemingly innocent JPEG file. This can allow the attacker access to the execution stack and execute arbitrary code in the target process. This attack pattern is a variant of standard buffer overflow attacks using an unexpected vector (binary files) to wrap its attack and open up a new attack vector. The attacker is required to either directly serve the binary content to the victim, or place it in a locale like a MP3 sharing application, for the victim to download. The attacker then is notified upon the download or otherwise locates the vulnerability opened up by the buffer overflow.","solutions":"Perform appropriate bounds checking on all buffers. Design: Enforce principle of least privilege Design: Static code analysis Implementation: Execute program in less trusted process space environment, do not allow lower integrity processes to write to higher integrity processes Implementation: Keep software patched to ensure that known vulnerabilities are not available for attackers to target on host.","name":"Overflow Binary Resource File","related_weakness":["119","120","697","713"],"id":"44"},{"prerequisites":"The attacker can create symbolic link on the target host. The target host does not perform correct boundary checking while consuming data from a resources.","summary":"This type of attack leverages the use of symbolic links to cause buffer overflows. An attacker can try to create or manipulate a symbolic link file such that its contents result in out of bounds data. When the target software processes the symbolic link file, it could potentially overflow internal buffers with insufficient bounds checking.","solutions":"Pay attention to the fact that the resource you read from can be a replaced by a Symbolic link. You can do a Symlink check before reading the file and decide that this is not a legitimate way of accessing the resource. Because Symlink can be modified by an attacker, make sure that the ones you read are located in protected directories. Pay attention to the resource pointed to by your symlink links (See attack pattern named \"Forced Symlink race\"), they can be replaced by malicious resources. Always check the size of the input data before copying to a buffer. Use a language or compiler that performs automatic bounds checking. Use an abstraction library to abstract away risky APIs. Not a complete solution. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Use OS-level preventative functionality. Not a complete solution.","name":"Buffer Overflow via Symbolic Links","related_weakness":["118","119","120","20","285","302","680","697","74"],"id":"45"},{"prerequisites":"The target program consumes user-controllable data in the form of tags or variables. The target program does not perform sufficient boundary checking.","summary":"This type of attack leverages the use of tags or variables from a formatted configuration data to cause buffer overflow. The attacker crafts a malicious HTML page or configuration file that includes oversized strings, thus causing an overflow.","solutions":"Use a language or compiler that performs automatic bounds checking. Use an abstraction library to abstract away risky APIs. Not a complete solution. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Use OS-level preventative functionality. Not a complete solution. Do not trust input data from user. Validate all user input.","name":"Overflow Variables and Tags","related_weakness":["118","119","120","20","680","697","733","74"],"id":"46"},{"prerequisites":"The program expands one of the parameters passed to a function with input controlled by the user, but a later function making use of the expanded parameter erroneously considers the original, not the expanded size of the parameter. The expanded parameter is used in the context where buffer overflow may become possible due to the incorrect understanding of the parameter size (i.e. thinking that it is smaller than it really is).","summary":"In this attack, the target software is given input that the attacker knows will be modified and expanded in size during processing. This attack relies on the target software failing to anticipate that the expanded data may exceed some internal limit, thereby creating a buffer overflow.","solutions":"Ensure that when parameter expansion happens in the code that the assumptions used to determine the resulting size of the parameter are accurate and that the new size of the parameter is visible to the whole system","name":"Buffer Overflow via Parameter Expansion","related_weakness":["118","119","120","130","131","20","680","697","74"],"id":"47"},{"prerequisites":"The target host exposes an API to the user. One or more API functions exposed by the target host has a buffer overflow vulnerability.","summary":"This attack targets libraries or shared code modules which are vulnerable to buffer overflow attacks. An attacker who has access to an API may try to embed malicious code in the API function call and exploit a buffer overflow vulnerability in the function's implementation. All clients that make use of the code library thus become vulnerable by association. This has a very broad effect on security across a system, usually affecting more than one software process.","solutions":"Use a language or compiler that performs automatic bounds checking. Use secure functions not vulnerable to buffer overflow. If you have to use dangerous functions, make sure that you do boundary checking. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Use OS-level preventative functionality. Not a complete solution.","name":"Buffer Overflow in an API Call","related_weakness":["118","119","120","20","680","697","733","74"],"id":"8"},{"prerequisites":"The target host exposes a command-line utility to the user. The command-line utility exposed by the target host has a buffer overflow vulnerability that can be exploited.","summary":"This attack targets command-line utilities available in a number of shells. An attacker can leverage a vulnerability found in a command-line utility to escalate privilege to root.","solutions":"Carefully review the service's implementation before making it available to user. For instance you can use manual or automated code review to uncover vulnerabilities such as buffer overflow. Use a language or compiler that performs automatic bounds checking. Use an abstraction library to abstract away risky APIs. Not a complete solution. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Operational: Use OS-level preventative functionality. Not a complete solution. Apply the latest patches to your user exposed services. This may not be a complete solution, especially against a zero day attack. Do not unnecessarily expose services.","name":"Buffer Overflow in Local Command-Line Utilities","related_weakness":["118","119","120","20","680","697","733","74"],"id":"9"}],"cwe":"CWE-119","Published":"2009-12-08T23:30:00","id":"CVE-2009-1298","cvss-time":"2018-10-10T19:35:00","cvss":7.8,"cvss-vector":"AV:N/AC:L/Au:N/C:N/I:N/A:C"}]}
     */
   /* public Json getCveInfo(String cve){
    	Json result = Json.object();
    	ArrayList<Json> resultTab = new ArrayList<Json>();
		try {
			resultTab.add(getCveDetails(cve));
		} catch (Exception e) {
			e.printStackTrace();
		}
		result.at("result",resultTab);
		return result;
	}*/
    
    /**
     * Récupère les informations pour une CVE d'ID donné en argument
     * @param cve L'ID de la CVE dont on veut récupèrer les informations
     * @return Un object JSON dont le champ "result" contient un tableau JSON d'object Json
     * Chacun de ces objets JSON décrit une CVE et contient les champs suivants :
     *     -summary
     *     -Modified
     *     -access
     *     -references
     *     -impact
     *     -assigner
     *     -statements
     *     -refmap
     *     -capec
     *     -cwe
     *     -Published
     *     -id
     *     -cvss-time
     *     -cvss
     *     -cvss-vector
     * Voir sur internet pour voir à quoi correspondent ces champs.  
     * Exemple : {"result":[{"summary":"The ip_frag_reasm function in net/ipv4/ip_fragment.c in the Linux kernel 2.6.32-rc8, and 2.6.29 and later versions before 2.6.32, calls IP_INC_STATS_BH with an incorrect argument, which allows remote attackers to cause a denial of service (NULL pointer dereference and hang) via long IP packets, possibly related to the ip_defrag function.","Modified":"2018-10-10T19:35:00","access":{"complexity":"LOW","vector":"NETWORK","authentication":"NONE"},"references":["http://git.kernel.org/?p=linux/kernel/git/torvalds/linux-2.6.git;a=commitdiff;h=bbf31bf18d34caa87dd01f08bf713635593697f2","http://lists.opensuse.org/opensuse-security-announce/2010-01/msg00000.html","http://secunia.com/advisories/37624","http://secunia.com/advisories/38017","http://twitter.com/spendergrsec/statuses/6339560349","http://wiki.rpath.com/Advisories:rPSA-2009-0161","http://www.kernel.org/pub/linux/kernel/v2.6/ChangeLog-2.6.32","http://www.mandriva.com/security/advisories?name=MDVSA-2009:329","http://www.osvdb.org/60788","http://www.securityfocus.com/archive/1/508517/100/0/threaded","http://www.theregister.co.uk/2009/12/11/linux_kernel_bugs_patched/","http://www.ubuntu.com/usn/USN-869-1","https://bugzilla.redhat.com/show_bug.cgi?id=544144","https://www.redhat.com/archives/fedora-package-announce/2009-December/msg00453.html","https://www.redhat.com/archives/fedora-package-announce/2009-December/msg00496.html"],"impact":{"integrity":"NONE","confidentiality":"NONE","availability":"COMPLETE"},"assigner":"cve@mitre.org","statements":[{"contributor":"Mark J Cox","lastmodified":"2009-12-09","organization":"Red Hat","statement":"Not vulnerable. This issue did not affect the versions of the Linux kernel as shipped with Red Hat Enterprise Linux 3, 4, 5 and Red Hat Enterprise MRG as they did not include upstream commit 7c73a6fa that introduced the problem."}],"refmap":{"confirm":["http://git.kernel.org/?p=linux/kernel/git/torvalds/linux-2.6.git;a=commitdiff;h=bbf31bf18d34caa87dd01f08bf713635593697f2","http://wiki.rpath.com/Advisories:rPSA-2009-0161","http://www.kernel.org/pub/linux/kernel/v2.6/ChangeLog-2.6.32","https://bugzilla.redhat.com/show_bug.cgi?id=544144"],"osvdb":["60788"],"secunia":["37624","38017"],"suse":["SUSE-SA:2010:001"],"mandriva":["MDVSA-2009:329"],"fedora":["FEDORA-2009-12786","FEDORA-2009-12825"],"ubuntu":["USN-869-1"],"bugtraq":["20091216 rPSA-2009-0161-1 hwdata kernel"],"misc":["http://twitter.com/spendergrsec/statuses/6339560349","http://www.theregister.co.uk/2009/12/11/linux_kernel_bugs_patched/"]},"capec":[{"prerequisites":"The application uses environment variables. An environment variable exposed to the user is vulnerable to a buffer overflow. The vulnerable environment variable uses untrusted data. Tainted data used in the environment variables is not properly validated. For instance boundary checking is not done before copying the input data to a buffer.","summary":"This attack pattern involves causing a buffer overflow through manipulation of environment variables. Once the attacker finds that they can modify an environment variable, they may try to overflow associated buffers. This attack leverages implicit trust often placed in environment variables.","solutions":"Do not expose environment variable to the user. Do not use untrusted data in your environment variables. Use a language or compiler that performs automatic bounds checking There are tools such as Sharefuzz [R.10.3] which is an environment variable fuzzer for Unix that support loading a shared library. You can use Sharefuzz to determine if you are exposing an environment variable vulnerable to buffer overflow.","name":"Buffer Overflow via Environment Variables","related_weakness":["118","119","120","20","302","680","697","733","74","99"],"id":"10"},{"prerequisites":"Targeted software performs buffer operations. Targeted software inadequately performs bounds-checking on buffer operations. Adversary has the capability to influence the input to buffer operations.","summary":"Buffer Overflow attacks target improper or missing bounds checking on buffer operations, typically triggered by input injected by an adversary. As a consequence, an adversary is able to write past the boundaries of allocated buffer regions in memory, causing a program crash or potentially redirection of execution as per the adversaries' choice.","solutions":"Use a language or compiler that performs automatic bounds checking. Use secure functions not vulnerable to buffer overflow. If you have to use dangerous functions, make sure that you do boundary checking. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Use OS-level preventative functionality. Not a complete solution. Utilize static source code analysis tools to identify potential buffer overflow weaknesses in the software.","name":"Overflow Buffers","related_weakness":["119","120","129","131","19","680","805"],"id":"100"},{"prerequisites":"The adversary must identify a programmatic means for interacting with a buffer, such as vulnerable C code, and be able to provide input to this interaction.","summary":"An adversary manipulates an application's interaction with a buffer in an attempt to read or modify data they shouldn't have access to. Buffer attacks are distinguished in that it is the buffer space itself that is the target of the attack rather than any code responsible for interpreting the content of the buffer. In virtually all buffer attacks the content that is placed in the buffer is immaterial. Instead, most buffer attacks involve retrieving or providing more input than can be stored in the allocated buffer, resulting in the reading or overwriting of other unintended program memory.","solutions":"To help protect an application from buffer manipulation attacks, a number of potential mitigations can be leveraged. Before starting the development of the application, consider using a code language (e.g., Java) or compiler that limits the ability of developers to act beyond the bounds of a buffer. If the chosen language is susceptible to buffer related issues (e.g., C) then consider using secure functions instead of those vulnerable to buffer manipulations. If a potentially dangerous function must be used, make sure that proper boundary checking is performed. Additionally, there are often a number of compiler-based mechanisms (e.g., StackGuard, ProPolice and the Microsoft Visual Studio /GS flag) that can help identify and protect against potential buffer issues. Finally, there may be operating system level preventative functionality that can be applied.","name":"Buffer Manipulation","related_weakness":["119"],"id":"123"},{"prerequisites":"The targeted client software communicates with an external server. The targeted client software has a buffer overflow vulnerability.","summary":"This type of attack exploits a buffer overflow vulnerability in targeted client software through injection of malicious content from a custom-built hostile service.","solutions":"The client software should not install untrusted code from a non-authenticated server. The client software should have the latest patches and should be audited for vulnerabilities before being used to communicate with potentially hostile servers. Perform input validation for length of buffer inputs. Use a language or compiler that performs automatic bounds checking. Use an abstraction library to abstract away risky APIs. Not a complete solution. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Ensure all buffer uses are consistently bounds-checked. Use OS-level preventative functionality. Not a complete solution.","name":"Client-side Injection-induced Buffer Overflow","related_weakness":["118","119","120","20","353","680","697","713","74"],"id":"14"},{"prerequisites":"Ability to control the length of data passed to an active filter.","summary":"In this attack, the idea is to cause an active filter to fail by causing an oversized transaction. An attacker may try to feed overly long input strings to the program in an attempt to overwhelm the filter (by causing a buffer overflow) and hoping that the filter does not fail securely (i.e. the user input is let into the system unfiltered).","solutions":"Make sure that ANY failure occurring in the filtering or input validation routine is properly handled and that offending input is NOT allowed to go through. Basically make sure that the vault is closed when failure occurs. Pre-design: Use a language or compiler that performs automatic bounds checking. Pre-design through Build: Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Operational: Use OS-level preventative functionality. Not a complete solution. Design: Use an abstraction library to abstract away risky APIs. Not a complete solution.","name":"Filter Failure through Buffer Overflow","related_weakness":["118","119","120","20","680","697","733","74"],"id":"24"},{"prerequisites":"The target system uses a mail server. Mail server vendor has not released a patch for the MIME conversion routine, the patch itself has a security hole or does not fix the original problem, or the patch has not been applied to the user's system.","summary":"An attacker exploits a weakness in the MIME conversion routine to cause a buffer overflow and gain control over the mail server machine. The MIME system is designed to allow various different information formats to be interpreted and sent via e-mail. Attack points exist when data are converted to MIME compatible format and back.","solutions":"Stay up to date with third party vendor patches From \"Exploiting Software\", please see reference below. Use the sendmail restricted shell program (smrsh) Use mail.local","name":"MIME Conversion","related_weakness":["119","120","20","74"],"id":"42"},{"prerequisites":"Target software processes binary resource files. Target software contains a buffer overflow vulnerability reachable through input from a user-controllable binary resource file.","summary":"An attack of this type exploits a buffer overflow vulnerability in the handling of binary resources. Binary resources may include music files like MP3, image files like JPEG files, and any other binary file. These attacks may pass unnoticed to the client machine through normal usage of files, such as a browser loading a seemingly innocent JPEG file. This can allow the attacker access to the execution stack and execute arbitrary code in the target process. This attack pattern is a variant of standard buffer overflow attacks using an unexpected vector (binary files) to wrap its attack and open up a new attack vector. The attacker is required to either directly serve the binary content to the victim, or place it in a locale like a MP3 sharing application, for the victim to download. The attacker then is notified upon the download or otherwise locates the vulnerability opened up by the buffer overflow.","solutions":"Perform appropriate bounds checking on all buffers. Design: Enforce principle of least privilege Design: Static code analysis Implementation: Execute program in less trusted process space environment, do not allow lower integrity processes to write to higher integrity processes Implementation: Keep software patched to ensure that known vulnerabilities are not available for attackers to target on host.","name":"Overflow Binary Resource File","related_weakness":["119","120","697","713"],"id":"44"},{"prerequisites":"The attacker can create symbolic link on the target host. The target host does not perform correct boundary checking while consuming data from a resources.","summary":"This type of attack leverages the use of symbolic links to cause buffer overflows. An attacker can try to create or manipulate a symbolic link file such that its contents result in out of bounds data. When the target software processes the symbolic link file, it could potentially overflow internal buffers with insufficient bounds checking.","solutions":"Pay attention to the fact that the resource you read from can be a replaced by a Symbolic link. You can do a Symlink check before reading the file and decide that this is not a legitimate way of accessing the resource. Because Symlink can be modified by an attacker, make sure that the ones you read are located in protected directories. Pay attention to the resource pointed to by your symlink links (See attack pattern named \"Forced Symlink race\"), they can be replaced by malicious resources. Always check the size of the input data before copying to a buffer. Use a language or compiler that performs automatic bounds checking. Use an abstraction library to abstract away risky APIs. Not a complete solution. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Use OS-level preventative functionality. Not a complete solution.","name":"Buffer Overflow via Symbolic Links","related_weakness":["118","119","120","20","285","302","680","697","74"],"id":"45"},{"prerequisites":"The target program consumes user-controllable data in the form of tags or variables. The target program does not perform sufficient boundary checking.","summary":"This type of attack leverages the use of tags or variables from a formatted configuration data to cause buffer overflow. The attacker crafts a malicious HTML page or configuration file that includes oversized strings, thus causing an overflow.","solutions":"Use a language or compiler that performs automatic bounds checking. Use an abstraction library to abstract away risky APIs. Not a complete solution. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Use OS-level preventative functionality. Not a complete solution. Do not trust input data from user. Validate all user input.","name":"Overflow Variables and Tags","related_weakness":["118","119","120","20","680","697","733","74"],"id":"46"},{"prerequisites":"The program expands one of the parameters passed to a function with input controlled by the user, but a later function making use of the expanded parameter erroneously considers the original, not the expanded size of the parameter. The expanded parameter is used in the context where buffer overflow may become possible due to the incorrect understanding of the parameter size (i.e. thinking that it is smaller than it really is).","summary":"In this attack, the target software is given input that the attacker knows will be modified and expanded in size during processing. This attack relies on the target software failing to anticipate that the expanded data may exceed some internal limit, thereby creating a buffer overflow.","solutions":"Ensure that when parameter expansion happens in the code that the assumptions used to determine the resulting size of the parameter are accurate and that the new size of the parameter is visible to the whole system","name":"Buffer Overflow via Parameter Expansion","related_weakness":["118","119","120","130","131","20","680","697","74"],"id":"47"},{"prerequisites":"The target host exposes an API to the user. One or more API functions exposed by the target host has a buffer overflow vulnerability.","summary":"This attack targets libraries or shared code modules which are vulnerable to buffer overflow attacks. An attacker who has access to an API may try to embed malicious code in the API function call and exploit a buffer overflow vulnerability in the function's implementation. All clients that make use of the code library thus become vulnerable by association. This has a very broad effect on security across a system, usually affecting more than one software process.","solutions":"Use a language or compiler that performs automatic bounds checking. Use secure functions not vulnerable to buffer overflow. If you have to use dangerous functions, make sure that you do boundary checking. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Use OS-level preventative functionality. Not a complete solution.","name":"Buffer Overflow in an API Call","related_weakness":["118","119","120","20","680","697","733","74"],"id":"8"},{"prerequisites":"The target host exposes a command-line utility to the user. The command-line utility exposed by the target host has a buffer overflow vulnerability that can be exploited.","summary":"This attack targets command-line utilities available in a number of shells. An attacker can leverage a vulnerability found in a command-line utility to escalate privilege to root.","solutions":"Carefully review the service's implementation before making it available to user. For instance you can use manual or automated code review to uncover vulnerabilities such as buffer overflow. Use a language or compiler that performs automatic bounds checking. Use an abstraction library to abstract away risky APIs. Not a complete solution. Compiler-based canary mechanisms such as StackGuard, ProPolice and the Microsoft Visual Studio /GS flag. Unless this provides automatic bounds checking, it is not a complete solution. Operational: Use OS-level preventative functionality. Not a complete solution. Apply the latest patches to your user exposed services. This may not be a complete solution, especially against a zero day attack. Do not unnecessarily expose services.","name":"Buffer Overflow in Local Command-Line Utilities","related_weakness":["118","119","120","20","680","697","733","74"],"id":"9"}],"cwe":"CWE-119","Published":"2009-12-08T23:30:00","id":"CVE-2009-1298","cvss-time":"2018-10-10T19:35:00","cvss":7.8,"cvss-vector":"AV:N/AC:L/Au:N/C:N/I:N/A:C"},{"summary":"wolfSSL before 4.5.0 mishandles TLS 1.3 server data in the WAIT_CERT_CR state, within SanityCheckTls13MsgReceived() in tls13.c. This is an incorrect implementation of the TLS 1.3 client state machine. This allows attackers in a privileged network position to completely impersonate any TLS 1.3 servers, and read or modify potentially sensitive information between clients using the wolfSSL library and these TLS servers.","Modified":"2020-08-24T22:15:00","access":{},"references":["https://research.nccgroup.com/2020/08/24/technical-advisory-wolfssl-tls-1-3-client-man-in-the-middle-attack/"],"impact":{},"assigner":"cve@mitre.org","cwe":"Unknown","last-modified":"2020-08-24T22:15:00","Published":"2020-08-24T22:15:00","id":"CVE-2020-24613","cvss":5.0}]} 
     */
   /* public Json getCveInfo(ArrayList<String> cveTab){
    	Json result = Json.object();
    	ArrayList<Json> resultTab = new ArrayList<Json>();
		for(String cve : cveTab) {
	    	try {
				resultTab.add(getCveDetails(cve));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		result.at("result",resultTab);
		return result;
	}*/
}