package io.gravitee.management.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyDevelopmentEntity {

    @JsonProperty("class")
    private String className;

    @JsonProperty("configuration_class")
    private String configuration;

    @JsonProperty("on_request_method")
    private String onRequestMethod;

    @JsonProperty("on_response_method")
    private String onResponseMethod;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getOnRequestMethod() {
        return onRequestMethod;
    }

    public void setOnRequestMethod(String onRequestMethod) {
        this.onRequestMethod = onRequestMethod;
    }

    public String getOnResponseMethod() {
        return onResponseMethod;
    }

    public void setOnResponseMethod(String onResponseMethod) {
        this.onResponseMethod = onResponseMethod;
    }
}
