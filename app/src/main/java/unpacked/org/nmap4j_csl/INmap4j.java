package org.nmap4j_csl;

import org.nmap4j_csl.core.nmap.ExecutionResults;
import org.nmap4j_csl.core.nmap.NMapExecutionException;
import org.nmap4j_csl.core.nmap.NMapInitializationException;
import org.nmap4j_csl.data.NMapRun;

public interface INmap4j {
	void execute() throws NMapInitializationException, NMapExecutionException;
	void addFlags(String flagSet);
	void includeHosts(String hosts);
	void excludeHosts(String hosts);
	String getOutput();
	NMapRun getResult();
	boolean hasError();
	ExecutionResults getExecutionResults();
}
