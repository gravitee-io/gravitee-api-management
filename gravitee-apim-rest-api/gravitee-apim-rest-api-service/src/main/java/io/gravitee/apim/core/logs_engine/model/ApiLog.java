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
package io.gravitee.apim.core.logs_engine.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ApiLog(
    String apiId,
    String apiName,
    OffsetDateTime timestamp,
    String id,
    String requestId,
    HttpMethod method,
    String clientIdentifier,
    BasePlan plan,
    BaseApplication application,
    String transactionId,
    Integer status,
    Boolean requestEnded,
    Integer gatewayResponseTime,
    String gateway,
    String uri,
    String endpoint,
    String message,
    String errorKey,
    String errorComponentName,
    String errorComponentType,
    List<ApiLogDiagnostic> warnings,
    Map<String, Object> additionalMetrics
) {
    public ApiLog withApiName(String apiName) {
        return new ApiLog(
            apiId,
            apiName,
            timestamp,
            id,
            requestId,
            method,
            clientIdentifier,
            plan,
            application,
            transactionId,
            status,
            requestEnded,
            gatewayResponseTime,
            gateway,
            uri,
            endpoint,
            message,
            errorKey,
            errorComponentName,
            errorComponentType,
            warnings,
            additionalMetrics
        );
    }

    public ApiLog withPlan(BasePlan plan) {
        return new ApiLog(
            apiId,
            apiName,
            timestamp,
            id,
            requestId,
            method,
            clientIdentifier,
            plan,
            application,
            transactionId,
            status,
            requestEnded,
            gatewayResponseTime,
            gateway,
            uri,
            endpoint,
            message,
            errorKey,
            errorComponentName,
            errorComponentType,
            warnings,
            additionalMetrics
        );
    }

    public ApiLog withApplication(BaseApplication application) {
        return new ApiLog(
            apiId,
            apiName,
            timestamp,
            id,
            requestId,
            method,
            clientIdentifier,
            plan,
            application,
            transactionId,
            status,
            requestEnded,
            gatewayResponseTime,
            gateway,
            uri,
            endpoint,
            message,
            errorKey,
            errorComponentName,
            errorComponentType,
            warnings,
            additionalMetrics
        );
    }

    public ApiLog withGateway(String gateway) {
        return new ApiLog(
            apiId,
            apiName,
            timestamp,
            id,
            requestId,
            method,
            clientIdentifier,
            plan,
            application,
            transactionId,
            status,
            requestEnded,
            gatewayResponseTime,
            gateway,
            uri,
            endpoint,
            message,
            errorKey,
            errorComponentName,
            errorComponentType,
            warnings,
            additionalMetrics
        );
    }
}
