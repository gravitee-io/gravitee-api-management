/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.debug.reactor.handler.context.steps;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.http.HttpHeaders;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DebugStepContent {

    private HttpHeaders headers;
    private Map<String, Serializable> attributes;
    private MultiValueMap<String, String> parameters;
    private MultiValueMap<String, String> pathParameters;
    private String path;
    private String contextPath;
    private HttpMethod method;
    private Integer status;
    private String reason;

    public DebugStepContent() {}

    public DebugStepContent headers(HttpHeaders headers) {
        this.headers = HttpHeaders.create(headers);
        return this;
    }

    public DebugStepContent attributes(Map<String, Serializable> attributes) {
        this.attributes = new HashMap<>(attributes);
        return this;
    }

    public DebugStepContent parameters(MultiValueMap<String, String> parameters) {
        this.parameters = new LinkedMultiValueMap<>(parameters);
        return this;
    }

    public DebugStepContent pathParameters(MultiValueMap<String, String> pathParameters) {
        this.pathParameters = new LinkedMultiValueMap<>(pathParameters);
        return this;
    }

    public DebugStepContent path(String path) {
        this.path = path;
        return this;
    }

    public DebugStepContent contextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public DebugStepContent method(HttpMethod method) {
        this.method = method;
        return this;
    }

    public DebugStepContent status(int status) {
        this.status = status;
        return this;
    }

    public DebugStepContent reason(String reason) {
        this.reason = reason;
        return this;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    public MultiValueMap<String, String> getParameters() {
        return parameters;
    }

    public MultiValueMap<String, String> getPathParameters() {
        return pathParameters;
    }

    public String getPath() {
        return path;
    }

    public String getContextPath() {
        return contextPath;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Integer getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
