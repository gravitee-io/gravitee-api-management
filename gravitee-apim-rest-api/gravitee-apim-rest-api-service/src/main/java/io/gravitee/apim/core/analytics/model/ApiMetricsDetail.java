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
package io.gravitee.apim.core.analytics.model;

import io.gravitee.apim.core.gateway.model.BaseInstance;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
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
    BaseApplicationEntity application;
    GenericPlanEntity plan;
    BaseInstance gateway;
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
}
