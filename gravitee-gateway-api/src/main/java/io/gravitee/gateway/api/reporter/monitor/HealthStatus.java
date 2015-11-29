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
package io.gravitee.gateway.api.reporter.monitor;

import io.gravitee.gateway.api.reporter.Reportable;

import java.time.Instant;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HealthStatus implements Reportable {

    private final String api;
    private final long timestamp;
    private int status;

    private HealthStatus(String api, long timestamp) {
        this.api = api;
        this.timestamp = timestamp;
    }

    public String getApi() {
        return api;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static Builder forApi(String api) {
        return new Builder(api);
    }

    @Override
    public Instant timestamp() {
        return Instant.ofEpochMilli(timestamp);
    }

    public static class Builder {

        private final String api;
        private long timestamp;

        private Builder(String api) {
            this.api = api;
        }

        public Builder on(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public HealthStatus build() {
            return new HealthStatus(api, timestamp);
        }
    }
}
