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
package io.gravitee.gateway.core.definition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ProxyDefinition {

    @JsonProperty("context_path")
    private String contextPath;

    @JsonProperty("target")
    private URI target;

    @JsonProperty("strip_context_path")
    private boolean stripContextPath;

    @JsonProperty("http_client")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private HttpClientDefinition httpClient;

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public URI getTarget() {
        return target;
    }

    public void setTarget(URI target) {
        this.target = target;
    }

    public boolean isStripContextPath() {
        return stripContextPath;
    }

    public void setStripContextPath(boolean stripContextPath) {
        this.stripContextPath = stripContextPath;
    }

    public HttpClientDefinition getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClientDefinition httpClient) {
        this.httpClient = httpClient;
    }
}
