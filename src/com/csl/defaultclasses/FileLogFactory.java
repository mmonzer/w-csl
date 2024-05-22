package com.csl.defaultclasses;

import java.util.function.LongSupplier;

import com.ucsl.interfaces.IFileLog;
import com.ucsl.interfaces.IFileLogFactory;

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
