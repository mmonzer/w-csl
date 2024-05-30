package com.ucsl.interfaces;

import java.util.List;

public interface IIDSMainProcessorParams {

	IIDSMainProcessor getIdsMainProcessor();
	IFileStoreService getFileUtils();
	String getIdsModelDir();
	String getRulesForSuricataBaseFileName();
	List<String> getTapsIDs();

}
