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
package io.gravitee.repository.log.v4.model.connection;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Carrier shape for an ES native-Kafka connection-metrics document. Repo-side stays schema-flexible — protocol-specific
 * fields land in {@link #additionalMetrics} keyed by the raw ES wire keys (see
 * {@link NativeApiMetricKeys}). The typed projection lives at the core boundary
 * ({@code io.gravitee.apim.core.log.model.NativeApiLog}); the boundary parser is
 * {@code NativeApiLogCrudServiceImpl.toDomain}.
 */
@Data
@Builder(toBuilder = true)
public class NativeApiMetrics {

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
    private Map<String, Object> additionalMetrics;
}
