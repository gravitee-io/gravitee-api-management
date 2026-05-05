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
package io.gravitee.apim.core.log.model;

import lombok.Builder;
import lombok.Data;

/**
 * Typed projection of a native API connection log entry. Built from the carrier-side
 * {@code io.gravitee.repository.log.v4.model.connection.NativeApiMetrics} (raw ES shape with
 * {@code Map<String,Object> additionalMetrics}); the flatten happens at
 * {@code NativeApiLogCrudServiceImpl.toDomain} / {@code toListDomain}.
 *
 * <p>Read-only projection — no construction-time invariants. Source-of-truth validation is at the ES write boundary.
 */
@Data
@Builder(toBuilder = true)
public class NativeApiLog {

    private String apiId;
    private String requestId;
    private String transactionId;
    private String timestamp;
    private String applicationId;
    private String planId;
    private String clientIdentifier;
    private String subscriptionId;
    private String entrypointId;
    private String gateway;
    private String remoteAddress;
    private String localAddress;
    private String host;
    private String errorKey;
    private String message;
    private NativeConnectionStatus connectionStatus;
    private String clientId;
    private String brokerId;
    private Long connectionDurationMs;
}
