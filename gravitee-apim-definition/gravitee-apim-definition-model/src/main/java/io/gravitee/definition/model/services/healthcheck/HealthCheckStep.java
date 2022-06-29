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
package io.gravitee.definition.model.services.healthcheck;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckStep implements Serializable {

    private String name = "default-step";

    @JsonProperty("request")
    private HealthCheckRequest request;

    @JsonProperty("response")
    private HealthCheckResponse response = HealthCheckResponse.DEFAULT_RESPONSE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HealthCheckRequest getRequest() {
        return request;
    }

    public void setRequest(HealthCheckRequest request) {
        this.request = request;
    }

    public HealthCheckResponse getResponse() {
        return response;
    }

    public void setResponse(HealthCheckResponse response) {
        this.response = response;
    }
}
