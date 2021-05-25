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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.definition.model.services.Services;
import java.io.Serializable;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointGroup implements Serializable {

    private String name;
    private Set<Endpoint> endpoints;

    @JsonProperty("load_balancing")
    private LoadBalancer loadBalancer = new LoadBalancer();

    private Services services = new Services();

    @JsonProperty("proxy")
    private HttpProxy httpProxy;

    @JsonProperty("http")
    private HttpClientOptions httpClientOptions = new HttpClientOptions();

    @JsonProperty("ssl")
    private HttpClientSslOptions httpClientSslOptions;

    private Map<String, String> headers;

    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Collection<Endpoint> endpoints) {
        if (endpoints == null) {
            this.endpoints = null;
            return;
        }
        this.endpoints = new LinkedHashSet<>();
        for (Endpoint endpoint : endpoints) {
            if (!this.endpoints.add(endpoint)) {
                throw new IllegalArgumentException("[api] API endpoint names must be unique");
            }
        }
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public Services getServices() {
        return services;
    }

    @JsonGetter("services")
    public Services getServicesJson() {
        if (services.isEmpty()) {
            return null;
        }
        return services;
    }

    @JsonSetter("services")
    public void setServices(Services services) {
        this.services = services;
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

    @JsonGetter("headers")
    public Map<String, String> getHeaders() {
        return headers;
    }

    @JsonIgnore
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @JsonSetter("headers")
    private void setHeadersJson(Map<String, String> headers) {
        if (this.headers != null) {
            if (headers != null) {
                headers.forEach(this.headers::putIfAbsent);
            }
        } else {
            this.headers = headers;
        }
    }

    @JsonSetter
    public void setHostHeader(String hostHeader) {
        if (!hostHeader.trim().isEmpty()) {
            Map<String, String> headers = Optional.ofNullable(getHeaders()).orElseGet(LinkedHashMap::new);
            headers.put(HttpHeaders.HOST, hostHeader);
            setHeaders(headers);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EndpointGroup that = (EndpointGroup) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
