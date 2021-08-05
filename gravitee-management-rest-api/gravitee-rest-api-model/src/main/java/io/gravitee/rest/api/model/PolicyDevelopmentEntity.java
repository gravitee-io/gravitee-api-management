/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model;

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
