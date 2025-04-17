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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class HttpRequest implements Serializable {

    @JsonProperty("path")
    private String path;

    @JsonProperty("method")
    private String method;

    @JsonProperty("body")
    private String body;

    @JsonProperty("headers")
    private Map<String, List<String>> headers;

    public HttpRequest(String path, String method) {
        this(path, method, null, null);
    }

    public HttpRequest(String path, String method, Map<String, List<String>> headers) {
        this(path, method, null, headers);
    }

    public HttpRequest(String path, String method, String body) {
        this(path, method, body, null);
    }

    public HttpRequest(String path, String method, String body, Map<String, List<String>> headers) {
        this.path = path;
        this.method = method;
        this.body = body;
        this.headers = headers;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public HttpRequest headers(Map<String, List<String>> headers) {
        this.headers = headers;
        return this;
    }

    public HttpRequest body(String body) {
        this.body = body;
        return this;
    }

    public HttpRequest method(String method) {
        this.method = method;
        return this;
    }

    public HttpRequest path(String path) {
        this.path = path;
        return this;
    }
}
