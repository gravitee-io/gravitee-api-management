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
package io.gravitee.apim.rest.api.mcp.core.model;

/**
 * Something that moved against the baseline, on a whole environment when {@code api} is null.
 */
public record Finding(Kind kind, EnvRef environment, ApiRef api, String summary) {
    public enum Kind {
        /** 5xx: the gateway or a backend fails. */
        SERVER_ERROR_RATE_UP,
        /** 4xx: requests are rejected, typically credentials, quotas or bad input. */
        CLIENT_ERROR_RATE_UP,
        LATENCY_UP,
        TRAFFIC_DROP,
    }
}
