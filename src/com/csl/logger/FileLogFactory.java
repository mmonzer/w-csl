package com.csl.logger;

import com.csl.defaultclasses.FileLog;
import com.ucsl.interfaces.IFileLog;
import com.ucsl.interfaces.IFileLogFactory;

import java.util.function.LongSupplier;

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
