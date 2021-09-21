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
package io.gravitee.definition.model.endpoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Deprecated(since = "3.13", forRemoval = true)
public class HttpEndpoint extends Endpoint {

    @JsonProperty("proxy")
    private HttpProxy httpProxy;

    @JsonProperty("http")
    private HttpClientOptions httpClientOptions;

    @JsonProperty("ssl")
    private HttpClientSslOptions httpClientSslOptions;

    @JsonProperty("headers")
    private List<HttpHeader> headers;

    @JsonProperty("healthcheck")
    private EndpointHealthCheckService healthCheck;

    public HttpEndpoint(String type, String name, String target) {
        super(type, name, target);
    }

    @JsonCreator
    public HttpEndpoint(@JsonProperty("name") String name, @JsonProperty("target") String target) {
        super("http", name, target);
    }

    public HttpProxy getHttpProxy() {
        return httpProxy;
    }

    public void setHttpProxy(HttpProxy httpProxy) {
        this.httpProxy = httpProxy;
    }

    public HttpClientOptions getHttpClientOptions() {
        return httpClientOptions;
    }

    public void setHttpClientOptions(HttpClientOptions httpClientOptions) {
        this.httpClientOptions = httpClientOptions;
    }

    public HttpClientSslOptions getHttpClientSslOptions() {
        return httpClientSslOptions;
    }

    public void setHttpClientSslOptions(HttpClientSslOptions httpClientSslOptions) {
        this.httpClientSslOptions = httpClientSslOptions;
    }

    public void setHeaders(List<HttpHeader> headers) {
        this.headers = headers;
    }

    public List<HttpHeader> getHeaders() {
        return headers;
    }

    public EndpointHealthCheckService getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(EndpointHealthCheckService healthCheck) {
        this.healthCheck = healthCheck;
    }
}
