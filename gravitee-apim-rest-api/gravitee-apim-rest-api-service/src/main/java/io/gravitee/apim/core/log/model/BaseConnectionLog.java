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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class BaseConnectionLog {

    private String apiId;
    private String requestId;
    private String timestamp;
    private String applicationId;
    private String planId;
    private String clientIdentifier;
    private String transactionId;
    private HttpMethod method;
    private int status;
    private boolean requestEnded;
    private String entrypointId;
    private String gateway;
    private String uri;
    private long gatewayResponseTime;
    private Long requestContentLength;
    private Long responseContentLength;
    private String message;
    private String errorKey;
    private String errorComponentName;
    private String errorComponentType;
    private List<ConnectionDiagnosticModel> warnings;
    /** The ID of the API Product associated with this connection log entry. Null when no product is associated. */
    private String apiProductId;

    /**
     * Resolved from {@code apiProductId} by {@code SearchApiV4ConnectionLogsUseCase} via {@code ApiProductQueryService}.
     * This field is intentionally null in the data foundation layer and will be populated in APIM-13545 ST-3.
     */
    private String apiProductName;
}
