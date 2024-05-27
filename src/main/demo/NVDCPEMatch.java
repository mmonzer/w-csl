package main.demo;

import java.text.ParseException;

import lib.unpacked.org.mitre.cpe.common.LogicalValue;
import lib.unpacked.org.mitre.cpe.common.WellFormedName;
import lib.unpacked.org.mitre.cpe.matching.CPENameMatcher;
import lib.unpacked.org.mitre.cpe.naming.CPENameUnbinder;

public class NVDCPEMatch {

	
	public static void main(String[] args) throws ParseException {
        // Examples.
        WellFormedName wfn = new WellFormedName("a", "microsoft", "internet_explorer", "8\\.0\\.6001", "beta", new LogicalValue("ANY"), "sp2", null, null, null, null);
        WellFormedName wfn2 = new WellFormedName("a", "microsoft", "internet_explorer", new LogicalValue("ANY"), new LogicalValue("ANY"), new LogicalValue("ANY"), new LogicalValue("ANY"), new LogicalValue("ANY"), new LogicalValue("ANY"), new LogicalValue("ANY"), new LogicalValue("ANY"));
        CPENameMatcher cpenm = new CPENameMatcher();
        System.out.println(cpenm.isDisjoint(wfn, wfn2)); // false
        System.out.println(cpenm.isEqual(wfn, wfn2)); // false
        System.out.println(cpenm.isSubset(wfn, wfn2)); // true, wfn2 is a subset of wfn
        System.out.println(cpenm.isSuperset(wfn, wfn2)); // false
        CPENameUnbinder cpenu = new CPENameUnbinder();
        wfn = cpenu.unbindFS("cpe:2.3:a:adobe:*:9.*:*:PalmOS:*:*:*:*:*");
        wfn2 = cpenu.unbindURI("cpe:/a::Reader:9.3.2:-:-");
        System.out.println(cpenm.isDisjoint(wfn, wfn2)); // true, wfn2 and wfn are disjoint
        System.out.println(cpenm.isEqual(wfn, wfn2)); // false 
        System.out.println(cpenm.isSubset(wfn, wfn2)); // false
        System.out.println(cpenm.isSuperset(wfn, wfn2)); // false

        WellFormedName wfn3 = cpenu.unbindURI("cpe:/o:linux:linux_kernel:2.6.32");
        
       System.out.println(wfn3.get("vendor"));
       
        System.out.println(wfn3);
    }
}
