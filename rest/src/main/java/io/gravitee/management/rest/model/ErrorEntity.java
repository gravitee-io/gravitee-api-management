package io.gravitee.management.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ErrorEntity {

    private String message;

    @JsonProperty("http_status")
    private int httpCode;

    public int getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
