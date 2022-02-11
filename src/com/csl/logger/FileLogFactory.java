package com.csl.logger;

import java.util.function.LongSupplier;

import com.xcsl.defaultclasses.FileLog;
import com.xcsl.interfaces.IFileLog;
import com.xcsl.interfaces.IFileLogFactory;

public class FileLogFactory implements IFileLogFactory {

	@Override
	public IFileLog createFileLog(String traceDir, String string, LongSupplier getSystemCurrentTimeMillis) {
		// TODO Auto-generated method stub
		
		return new FileLog(traceDir, string, getSystemCurrentTimeMillis);
	}
	@Override
	public IFileLog createFileLog(String dir, String filename, long max_size, LongSupplier getSystemCurrentTimeMillis) {
		// TODO Auto-generated method stub
		return new FileLog(dir, filename,max_size, getSystemCurrentTimeMillis);
	}
	

}
