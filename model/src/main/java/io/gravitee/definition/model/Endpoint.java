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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Endpoint {

    public static int DEFAULT_WEIGHT = 1;

    private String name;

    private String target;

    private int weight = DEFAULT_WEIGHT;

    private boolean backup;

    private boolean healthcheck = true;

    private Status status = Status.UP;

    private HttpProxy httpProxy;

    private HttpClientOptions httpClientOptions;

    private HttpClientSslOptions httpClientSslOptions;

    private String tenant;

    public Endpoint(String name, String target) {
        this();
        this.name = name;
        this.target = target;
    }

    public Endpoint() {
        this.weight = DEFAULT_WEIGHT;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isBackup() {
        return backup;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }

    public boolean isHealthcheck() {
        return healthcheck;
    }

    public void setHealthcheck(boolean healthcheck) {
        this.healthcheck = healthcheck;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Endpoint endpoint = (Endpoint) o;

        return name.equals(endpoint.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public enum Status {
        UP(3),
        DOWN(0),
        TRANSITIONALLY_DOWN(1),
        TRANSITIONALLY_UP(2);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }
    }
}
