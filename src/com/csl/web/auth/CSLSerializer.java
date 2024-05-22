package com.csl.web.auth;

import com.ucsl.json.Json;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;

public class CSLSerializer implements Serializer {

	@Override
	public byte[] serialize(Object t) throws SerializationException {
		// TODO Auto-generated method stub
		System.out.println("CONVERT:"+t);
		return Json.make(t).toString().getBytes();
	}

}
