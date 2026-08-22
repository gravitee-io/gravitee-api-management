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
package io.gravitee.rest.api.model.v4.analytics;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiMetricsDetail {

    String timestamp;
    String apiId;
    String requestId;
    String transactionId;
    String host;
    String applicationId;
    String planId;
    String subscriptionId;
    String gateway;
    String uri;
    int status;
    long requestContentLength;
    long responseContentLength;
    String remoteAddress;
    long gatewayLatency;
    long gatewayResponseTime;
    long endpointResponseTime;
    HttpMethod method;
    String endpoint;
    String message;
    String errorKey;
    String errorComponentName;
    String errorComponentType;
    List<ConnectionDiagnosticModel> warnings;
    /** Credential type the connection authenticated with. Top-level in the document, not an additional metric. */
    String securityType;
    /**
     * Token identifying the authenticated credential, read from the {@code security-token} root field that
     * every API type writes — not a native-only field.
     *
     * <p><b>May hold a credential verbatim.</b> On an HTTP document produced by an API-key plan this is the raw
     * API key ({@code ApiKeyAuthenticationHandler} calls {@code setSecurityToken(apiKey)}). Only the native
     * Kafka reactor guarantees otherwise: it reports the plan type but never the key value, so the token there
     * is the non-PII OAuth/JWT client id. Anything projecting this field outside a native connection must
     * scope or redact it.
     */
    String securityToken;
    Map<String, Object> additionalMetrics;
}
