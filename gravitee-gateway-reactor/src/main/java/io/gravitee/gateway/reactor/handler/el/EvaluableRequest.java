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
package io.gravitee.gateway.reactor.handler.el;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;

import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EvaluableRequest {

    private final Request request;

    public EvaluableRequest(Request request) {
        this.request = request;
    }

    public String getId() {
        return request.id();
    }

    public HttpHeaders getHeaders() {
        return request.headers();
    }

    public Map<String, String> getParams() {
        return request.parameters();
    }

    public String[] getPaths() {
        return request.path().split("/");
    }
}
