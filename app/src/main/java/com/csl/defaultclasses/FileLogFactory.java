package com.csl.defaultclasses;

import com.ucsl.interfaces.IFileLog;
import com.ucsl.interfaces.IFileLogFactory;

import java.util.function.LongSupplier;

public class FileLogFactory implements IFileLogFactory {

	@Override
	public IFileLog createFileLog(String dir, String string, LongSupplier getSystemCurrentTimeMillis) {
		return new FileLog(dir, string, getSystemCurrentTimeMillis);
	}

	@Override
	public IFileLog createFileLog(String dir, String filename, long max_size, LongSupplier getSystemCurrentTimeMillis) {
		return new FileLog(dir, filename,max_size, getSystemCurrentTimeMillis);
	}

}
