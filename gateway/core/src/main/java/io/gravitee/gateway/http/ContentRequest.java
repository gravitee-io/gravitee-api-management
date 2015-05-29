package io.gravitee.gateway.http;

import java.io.InputStream;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ContentRequest extends Request {

    private final InputStream inputStream;

    private long contentLength;

    public ContentRequest(InputStream is) {
        this.inputStream = is;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public boolean hasContent() {
        return true;
    }
}
