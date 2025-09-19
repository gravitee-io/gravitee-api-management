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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.definition.model.services.Services;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointGroup implements Serializable {

    @JsonProperty("name")
    private String name;

    @JsonProperty("endpoints")
    private Set<Endpoint> endpoints;

    @JsonProperty("load_balancing")
    @Builder.Default
    private LoadBalancer loadBalancer = new LoadBalancer();

    @JsonProperty("services")
    @Builder.Default
    private Services services = new Services();

    @JsonProperty("proxy")
    private HttpProxy httpProxy;

    @JsonProperty("http")
    @Builder.Default
    private HttpClientOptions httpClientOptions = new HttpClientOptions();

    @JsonProperty("ssl")
    private HttpClientSslOptions httpClientSslOptions;

    @JsonProperty("headers")
    private List<HttpHeader> headers;

    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Set<Endpoint> endpoints) {
        this.endpoints = endpoints;
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

    public Services getServices() {
        return services;
    }

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

    public List<HttpHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HttpHeader> headers) {
        this.headers = headers;
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

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Stream.of(
            Optional.ofNullable(this.endpoints)
                .map(e -> e.stream().map(Endpoint::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
                .orElse(List.of()),
            Optional.ofNullable(this.services).map(Services::getPlugins).orElse(List.of())
        )
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
