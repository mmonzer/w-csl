package com.xcsl.interfaces;

import java.util.function.LongSupplier;

public interface IFileLogFactory {

	IFileLog createFileLog(String traceDir, String string, LongSupplier getSystemCurrentTimeMillis);

	IFileLog createFileLog(String dir, String filename, long max_size, LongSupplier getSystemCurrentTimeMillis) ;
		


	
}
