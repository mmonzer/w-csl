package com.csl.defaultclasses;

import com.ucsl.interfaces.ICSLFile;

import java.io.*;

public class CSLFile implements ICSLFile {
	File f=null;
	BufferedReader b=null;

	@Override
	public boolean open(String fileName) {

		File f= new File(fileName);

		try {
			b = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {

			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public String readLine() {
		if (b==null) return null;
		try {
			return b.readLine();
		} catch (IOException e) {

			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void close() {

		try {
			if ((f!=null)&&(b!=null))	b.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}

	

}
