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

import io.gravitee.common.http.HttpMethod;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ConnectionLog {

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
    private long requestContentLength;
    private long responseContentLength;
    private String endpoint;
}
