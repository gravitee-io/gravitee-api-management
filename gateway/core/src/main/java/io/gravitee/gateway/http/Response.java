package io.gravitee.gateway.http;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Response {

    private int status;

    private Map<String, String> headers = new HashMap();

    private OutputStream os = new ByteArrayOutputStream(5120);

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public void setOutputStream(OutputStream os) {
        this.os = os;
    }
}
