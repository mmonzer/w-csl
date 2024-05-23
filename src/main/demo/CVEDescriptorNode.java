package main.demo;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.mitre.cpe.common.WellFormedName;
import org.mitre.cpe.matching.CPENameMatcher;
import org.mitre.cpe.naming.CPENameUnbinder;

public class CVEDescriptorNode {
	
	static CPENameUnbinder cpenu = new CPENameUnbinder();
	static     CPENameMatcher cpenm = new CPENameMatcher();


	int type=0; // 1: OR, 2: AND, 3: TERM
	List<CVEDescriptorNode> nodes=null;
	String cpeUri=null;

	public int getSize() {
		int n=12+4*3;
		if (type==3) return cpeUri.length()+24+n;
		if ((type==2)||(type==1)) {
			for (CVEDescriptorNode node:nodes) {
				n=n+4+node.getSize();
			}
		}
		return n;
	}
	
	public CVEDescriptorNode(String cpeUri) {
		type=3;
		this.cpeUri=cpeUri;
	}
	
	public CVEDescriptorNode() {
		type=1;
		this.nodes= new ArrayList<CVEDescriptorNode>();		
	}
	
	public CVEDescriptorNode setAndOperator() {
		if (type!=1) {
			System.err.println("Invalid op setAnd");
			return this;
		}
		type=2;
		return this;
	}
	
	public CVEDescriptorNode add(CVEDescriptorNode node) {
		if ((type<1)||(type>2)) {
			System.err.println("Invalid op addNode");
			return this;
		}
		nodes.add(node);
		return this;
	}
	
	
	public boolean contains(WellFormedName key) {

		if (type!=3) {
			System.err.println("Invalid node "+this);
		}
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
	
	public boolean containsAny(List<WellFormedName> keys) {

		if (type!=3) {
			System.err.println("Invalid node "+this);
		}
		for (WellFormedName key:keys) {
			if (contains(key)) return true;
		}
		return false;
	}
	
	
	public boolean eval(List<WellFormedName> keys) {

		if (type==3) {
			return containsAny(keys);
		}
		else if (type==1) {
			for (CVEDescriptorNode n:nodes) {
				if (n.eval(keys)) return true;
			}
			return false;
		}
		else if (type==2) {
			for (CVEDescriptorNode n:nodes) {
				if (!n.eval(keys)) return false;
			}
			return true;
		}
		
		return  false; // invalid
	}
	
	
	public String toString() {
		if (type==3) return "cpeUri:"+cpeUri;
		if (type<1) return "Invalid node";
		if (type>3) return "Invalid node";
		String s="";
		for (CVEDescriptorNode n:nodes) {
			if (!s.isEmpty()) s=s+",";
		}
		if (type==1) s="OR("+s+")";
		if (type==2) s="AND("+s+")";
		
		return s;
	}
}
