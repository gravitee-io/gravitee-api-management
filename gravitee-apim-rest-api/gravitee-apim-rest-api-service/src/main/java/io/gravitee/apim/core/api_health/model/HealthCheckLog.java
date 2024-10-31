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
package io.gravitee.apim.core.api_health.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder(toBuilder = true)
public record HealthCheckLog(
    String id,
    Instant timestamp,
    String apiId,
    String endpointName,
    String gatewayId,
    long responseTime,
    boolean success,
    List<Step> steps
) {
    public record Step(String name, boolean success, String message, Request request, Response response) {}
    public record Request(String uri, String method, Map<String, String> headers) {}
    public record Response(int status, String body, Map<String, String> headers) {}
}
