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
package io.gravitee.gateway.handlers.api.metrics;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathMappingMetricsHandler implements Handler<Response> {

    private final Handler<Response> next;
    private final Map<String, Pattern> mapping;
    private final Request request;

    public PathMappingMetricsHandler(final Handler<Response> next, final Map<String, Pattern> mapping, Request request) {
        this.next = next;
        this.mapping = mapping;
        this.request = request;
    }

    @Override
    public void handle(Response result) {
        mapping.entrySet().stream()
                .filter(regexMappedPath -> regexMappedPath.getValue().matcher(request.pathInfo()).matches())
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(resolvedMappedPath -> request.metrics().setMappedPath(resolvedMappedPath));

        next.handle(result);
    }
}
