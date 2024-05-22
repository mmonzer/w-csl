package com.csl.web.proxy;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import spark.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static spark.Spark.*;


public class ProxyServer {

	
	static public String formatPath(String proxyPath) {
		if (proxyPath == null)
			proxyPath = "/";
		if (!proxyPath.endsWith("/"))
			proxyPath += "/";
		return proxyPath;
	}
	
	static public String url(spark.Request req, String proxyServer, String proxyPath) {
		String proxyUrl = proxyServer + "/"  + req.pathInfo().replace(proxyPath, "");
		return (req.queryString() == null) ? proxyUrl : (proxyUrl + "?" + req.queryString());
	}

	static public void addHeader(Request request, spark.Request req) {
		for (String header : req.headers()) {
			if (!header.equals("Content-Length"))
				request.setHeader(header, req.headers(header));
		}
	}

	static public void addBody(Request request, spark.Request req) {
		request.bodyByteArray(req.bodyAsBytes());
	}

	static public  HttpResponse go(Request request) throws ClientProtocolException, IOException {
		return request.execute().returnResponse();
	}

	static public  void mapHeaders(HttpResponse response, spark.Response res) {
		for (Header header : response.getAllHeaders()) {
			res.header(header.getName(), header.getValue());
		}
	}

	static public  void mapStatus(HttpResponse response, spark.Response res) {
		res.status(response.getStatusLine().getStatusCode());
	}

	/*
	 * This had issues with binary data
	 * private static String result(HttpResponse response) throws ParseException,
	 * IOException { HttpEntity entity = response.getEntity(); return entity == null
	 * ? "" : EntityUtils.toString(entity); }
	 */

	static public  void extractResponse(HttpResponse httpResponse, HttpServletResponse raw) throws IOException {
		HttpEntity entity = httpResponse.getEntity();
		if (entity == null)
			return;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		entity.writeTo(baos);
		byte[] bytes = baos.toByteArray();
		raw.getOutputStream().write(bytes);
		raw.getOutputStream().flush();
		raw.getOutputStream().close();
	}
	/**
	 * 
	 * @param service
	 *            - instance of Service for which proxy has to be enabled
	 * @param proxyServer
	 *            - URL of the server to proxy requests to
	 * @param proxyPath
	 *            - local path for which requests have to be forwarded; null = '/'
	 */
	public static void enableProxy(Service service, String proxyServer, String proxyPath) {

		String path= formatPath(proxyPath);
		String pathFilter = proxyPath + "*";

		service.get(pathFilter, (req, res) -> {
			Request request = Request.Get(url(req, proxyServer, path));
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			extractResponse(response, res.raw());
			return res.raw();
		});

		service.post(pathFilter, (req, res) -> {
			Request request = Request.Post(url(req, proxyServer, path));
			addBody(request, req);
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			extractResponse(response, res.raw());
			return res.raw();
		});

		service.put(pathFilter, (req, res) -> {
			Request request = Request.Put(url(req, proxyServer, path));
			addBody(request, req);
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			extractResponse(response, res.raw());
			return res.raw();
		});

		service.delete(pathFilter, (req, res) -> {
			Request request = Request.Delete(url(req, proxyServer, path));
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			extractResponse(response, res.raw());
			return res.raw();
		});

		service.options(pathFilter, (req, res) -> {
			Request request = Request.Options(url(req, proxyServer, path));
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			return res.raw();
		});

		service.head(pathFilter, (req, res) -> {
			Request request = Request.Head(url(req, proxyServer, path));
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			return res.raw();
		});

	}

	/**
	 * 
	 * @param proxyServer
	 *            - URL of the server to proxy requests to
	 * @param proxyPath
	 *            - local path for which requests have to be forwarded; null = '/'
	 */
	public static void enableProxy(String proxyServer, String proxyPath) {
		
		String path = formatPath(proxyPath);
		String pathFilter = proxyPath + "*";
		String pathFilterWithoutEnd = proxyPath.substring(0,proxyPath.length()-1);

		get(pathFilter, (req, res) -> {
			System.out.println("Proxy get :"+req+" path="+path+" server"+proxyServer+path);
			Request request = Request.Get(url(req, proxyServer+path, path));
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			extractResponse(response, res.raw());
			return res.raw();
		});

		post(pathFilterWithoutEnd, (req, res) -> {
			System.out.println("Proxy post :"+req+" path="+path+" server"+proxyServer);
			
			Request request = Request.Post(url(req, proxyServer, path));
			addBody(request, req);
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			extractResponse(response, res.raw());
			return res.raw();
		});

		put(pathFilter, (req, res) -> {
			Request request = Request.Put(url(req, proxyServer, path));
			addBody(request, req);
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			extractResponse(response, res.raw());
			return res.raw();
		});

		delete(pathFilter, (req, res) -> {
			Request request = Request.Delete(url(req, proxyServer, path));
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			extractResponse(response, res.raw());
			return res.raw();
		});

		options(pathFilter, (req, res) -> {
			Request request = Request.Options(url(req, proxyServer, path));
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			return res.raw();
		});

		head(pathFilter, (req, res) -> {
			Request request = Request.Head(url(req, proxyServer, path));
			addHeader(request, req);
			HttpResponse response = go(request);
			mapHeaders(response, res);
			mapStatus(response, res);
			return res.raw();
		});

	}
}

