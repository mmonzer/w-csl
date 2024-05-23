package main.demo;

import java.util.ArrayList;
import java.util.List;

import org.mitre.cpe.common.WellFormedName;

import com.ucsl.json.Json;


public class CVEDescriptor {

	CVEDescriptorNode nodeExp;
	String ID="";
	String CWE="";
	String publishedDate="";
	String lastModifiedDate="";
	String description="";
	String vectorString="";
	double cvss;
	
	public CVEDescriptor() {
		// TODO Auto-generated constructor stub
	}
	
	
	public int getSize() {
		int n=0;
		n=n+ID.length()+CWE.length()+publishedDate.length()+lastModifiedDate.length()+description.length()+vectorString.length();
		return n+24*6+12+4*7;
	}
	
	public CVEDescriptor initFromCveJson(Json item) {
		
		Json nodes=item.get("configurations").get("nodes");
		//expSet=toExpSet(nodes);
		nodeExp= toNode(nodes);
		ID=item.get("cve").get("CVE_data_meta").get("ID").asString();
		CWE=getCWE(item.get("cve").get("problemtype").get("problemtype_data"));
		description=getDescription(item.get("cve").get("description").get("description_data"));
		
		vectorString=getVectorString(item);
		publishedDate=item.get("publishedDate").asString();
		lastModifiedDate=item.get("lastModifiedDate").asString();
		
		return this;
	}
	
	String getVectorString(Json item) {
		//vectorString=item.get("impact").get("baseMetricV3").get("cvssV3").get("vectorString").asString();
		
		Json j=item.get("impact");
		if (j==null) { System.err.println("No Impact Vector string in "+item);return "";}
		j=j.get("baseMetricV3");
		if (j==null) {
			j=item.get("impact");
			j=j.get("baseMetricV2");
			if (j==null) { 
				//System.err.println("No baseMetricsV3 and V2 Vector string in "+item);
				return "";}
			j=j.get("cvssV2");
			if (j==null) { System.err.println("No cvssV2 Vector string in "+item);return "";}
			cvss=j.get("baseScore").asDouble();		
			j=j.get("vectorString");
			if (j==null) { System.err.println("No Vector string in "+item);return "";}
	
			return "V2/"+j.asString();
			
		}
			
		j=j.get("cvssV3");
		if (j==null) { 
			System.err.println("No cvssV3 Vector string in "+item);
			return "";
		}
		j=j.get("vectorString");
		if (j==null) { 
			System.err.println("No Vector string in "+item);
			return "";
		}
		return "V3/"+j.asString();
		
	}
	
	public String getCWE(Json problemtype_data_array) {
		String s="";
		for (Json problemtype_data:problemtype_data_array.asJsonList()) {
			Json description_array=problemtype_data.get("description");
			for (Json description:description_array.asJsonList()) {
				if (!s.isEmpty()) s=s+';';
				s=s+description.get("value").asString();
			}
		}
		return s;
	}
	
	
	public String getDescription(Json description_data_array) {
		String s="";
		for (Json description_data:description_data_array.asJsonList()) {
				if (!s.isEmpty()) s=s+"$$$";
				s=s+description_data.get("value").asString();
			}
		return s;
	}
	
	
	public  List<String> toExpSet(Json listOfterms ) {
		List<String> result= new ArrayList<String>(listOfterms.asJsonList().size());
		for (Json term:listOfterms.asJsonList()) {
			result.add(toExp(term));
		}
		return result;
	}
	
	public  String toExp(Json term ) {
		String s="";
		Json op=term.get("operator");

		if (op==null) {
			String sx=term.get("cpe23Uri").asString();
			if (sx.contains(" ")) System.err.println("Error char & in :"+sx);
			return "CPE "+sx;
			
		}
		String sop=op.asString();

		if (sop.compareTo("OR")==0) {
			s=s+"OR_START ";
			for (Json x:term.get("cpe_match").asJsonList()) {
				if (!s.endsWith(" ")) s=s+" ";
				s=s+toExp(x);
				
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
				s=s+toExp(x);
				if (!s.endsWith(" ")) s=s+" ";
			}
			if (!s.endsWith(" ")) s=s+" ";
			s=s+"AND ";
			return s;
		}
		else
			return ""+term.toString();
	}
	
	public  CVEDescriptorNode toNode(Json listOfterms ) {
		CVEDescriptorNode node= new CVEDescriptorNode();
		for (Json term:listOfterms.asJsonList()) {
			node.add(termToNode(term));
		}
		return node;
	}
	
	public  CVEDescriptorNode termToNode(Json term ) {
		Json op=term.get("operator");
		if (op==null) {
			String sx=term.get("cpe23Uri").asString();
			return new CVEDescriptorNode(sx);
		}
		String sop=op.asString();
		if (sop.compareTo("OR")==0) {
			CVEDescriptorNode node=new CVEDescriptorNode();
			for (Json x:term.get("cpe_match").asJsonList()) {
				node.add(termToNode(x));
			}	
			return node;
		}
		else if (sop.compareTo("AND")==0) {
			CVEDescriptorNode node=new CVEDescriptorNode().setAndOperator();
			Json termsList=term.get("children");
			if (termsList==null) {
				termsList=term.get("cpe_match");
			}
			for (Json x:termsList.asJsonList()) {
				node.add(termToNode(x));
			}
			return node;
		}
		else {
			System.err.println("Invalid tem"+term);
			return new CVEDescriptorNode("");
		}
		
	}
	
	public Json toJson() {
		
		Json j=Json.object();
		j.at("id",ID);
		j.at("cwe",CWE);
		j.at("cvss",cvss);
		j.at("summary",description);
		j.at("vectorString",vectorString);
		j.at("publishedDate", publishedDate);
		j.at("lastModifiedDate", lastModifiedDate);
		return j;
		
	}
	
	public CVEDescriptor fromJson(Json j) {
		return this;
	}


	public boolean eval(List<WellFormedName> keys) {
		return nodeExp.eval(keys);
	}
}