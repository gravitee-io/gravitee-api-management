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

import java.time.Instant;

/**
 * @param errorKey the reason the gateway recorded when it produced the response itself; null when the backend answered
 * @param errorComponent the policy or connector that failed the request, when the gateway knows it
 */
public record FailedRequest(
    Instant at,
    ApiRef api,
    String method,
    String uri,
    Integer status,
    String errorKey,
    String errorComponent,
    String message,
    String application,
    String plan,
    String gateway,
    Integer responseTimeMs,
    String requestId
) {}
