package com.csl.web.auth;

import com.ucsl.json.Json;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;

import java.io.IOException;
import java.io.OutputStream;

public class CSLSerializer implements Serializer {

	@Override
	public byte[] serialize(Object t) throws SerializationException {
		// TODO Auto-generated method stub
		System.out.println("CONVERT:"+t);
		return Json.make(t).toString().getBytes();
	}
	@Override
	public void serialize(Object o, OutputStream outputStream) throws SerializationException {
		try {
			byte[] bytes = Json.make(o).toString().getBytes();
			outputStream.write(bytes);
		} catch (IOException e) {
			throw new SerializationException("Failed to serialize object to output stream", e);
		}
	}

}

