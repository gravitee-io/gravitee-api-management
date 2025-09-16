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
package io.gravitee.rest.api.model.v4.log.connection;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class ConnectionLogDetail {

    private String requestId;
    private String apiId;
    private String timestamp;
    private String clientIdentifier;
    private boolean requestEnded;
    private Request entrypointRequest;
    private Request endpointRequest;
    private Response entrypointResponse;
    private Response endpointResponse;
    private String message;
    private String errorKey;
    private String errorComponentName;
    private String errorComponentType;
    private List<ConnectionDiagnosticModel> warnings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Request {

        private String method;
        private String uri;
        private Map<String, List<String>> headers;
        private String body;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Response {

        private int status;
        private Map<String, List<String>> headers;
        private String body;
    }
}
