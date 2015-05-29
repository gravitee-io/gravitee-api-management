package io.gravitee.gateway.http;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Request {

	private final String id;

	private String remoteAddr;

	private String method;

	private String destination;

	private boolean secure;

	private Map<String, String> queryParameters = new LinkedHashMap();

	private Map<String, String> headers = new HashMap();

	public Request() {
		this.id = UUID.randomUUID().toString();
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public Map<String, String> getQueryParameters() {
		return queryParameters;
	}

	public void setQueryParameters(Map<String, String> queryParameters) {
		this.queryParameters = queryParameters;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getId() {
		return id;
	}

	public boolean hasContent() {
		return false;
	}
}
