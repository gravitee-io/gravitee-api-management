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
package io.gravitee.apim.plugin.apiservice.healthcheck.http;

import static io.gravitee.plugin.apiservice.healthcheck.common.HealthCheckManagedEndpoint.DEFAULT_FAILURE_THRESHOLD;
import static io.gravitee.plugin.apiservice.healthcheck.common.HealthCheckManagedEndpoint.DEFAULT_SUCCESS_THRESHOLD;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.reactive.api.apiservice.ApiServiceConfiguration;
import java.util.List;
import lombok.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HttpHealthCheckServiceConfiguration implements ApiServiceConfiguration {

    @JsonProperty("schedule")
    private String schedule;

    @JsonProperty("target")
    private String target;

    @JsonProperty("method")
    private HttpMethod method;

    @JsonProperty("headers")
    private List<HttpHeader> headers;

    @JsonProperty("body")
    private String body;

    @JsonProperty("overrideEndpointPath")
    private boolean overrideEndpointPath;

    @JsonProperty("successThreshold")
    private int successThreshold = DEFAULT_SUCCESS_THRESHOLD;

    @JsonProperty("failureThreshold")
    private int failureThreshold = DEFAULT_FAILURE_THRESHOLD;

    @JsonProperty("assertion")
    private String assertion;
}
