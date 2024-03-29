/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckResponse implements Serializable {

    public static final HealthCheckResponse DEFAULT_RESPONSE = new HealthCheckResponse();
    public static final String DEFAULT_ASSERTION = "#response.status == 200";

    @JsonProperty("assertions")
    private List<String> assertions = Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION);

    public List<String> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<String> assertions) {
        this.assertions = assertions;
    }
}
