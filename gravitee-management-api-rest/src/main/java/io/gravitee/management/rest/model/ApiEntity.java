package io.gravitee.management.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Titouan COMPIEGNE
 */
public class ApiEntity {

    @JsonProperty("api_id")
    private String apiId;

    @JsonProperty("is_synchronized")
    private boolean isSynchronized;

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public boolean getIsSynchronized() {
        return isSynchronized;
    }

    public void setIsSynchronized(boolean isSynchronized) {
        this.isSynchronized = isSynchronized;
    }

}
