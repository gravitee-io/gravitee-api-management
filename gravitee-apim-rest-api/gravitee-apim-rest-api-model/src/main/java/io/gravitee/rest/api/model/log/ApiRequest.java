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
package io.gravitee.rest.api.model.log;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.log.extended.Request;
import io.gravitee.rest.api.model.log.extended.Response;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class ApiRequest {

    private String id;
    private String api;
    private long timestamp;
    private String transactionId;
    private String uri;
    private String path;
    private HttpMethod method;
    private int status;
    private long responseTime;
    private long apiResponseTime;
    private long requestContentLength;
    private long responseContentLength;
    private String plan;
    private String application;
    private String localAddress;
    private String remoteAddress;
    private String endpoint;
    private String tenant;
    private Request clientRequest, proxyRequest;
    private Response clientResponse, proxyResponse;
    private String message;
    private String gateway;
    private String subscription;
    private Map<String, Map<String, String>> metadata;
    private String host;
    private String user;
    private String securityType;
    private String securityToken;
    private String errorKey;
    private String errorComponentName;
    private String errorComponentType;
    private List<DiagnosticItem> warnings;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiRequest request = (ApiRequest) o;

        return id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
