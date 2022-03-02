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
package io.gravitee.gateway.debug.reactor.handler.http;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.debug.reactor.handler.context.PathTransformer;
import io.gravitee.gateway.reactor.handler.http.ContextualizedHttpServerRequest;

/**
 * Transforms the debug request to get rid of the debug eventId.
 * This id has to be removed from contextPath and request's path.
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ContextualizedDebugHttpServerRequest extends ContextualizedHttpServerRequest {

    private final String contextPath;
    private final String path;

    public ContextualizedDebugHttpServerRequest(String contextPath, Request request, String eventId) {
        super(contextPath, request);
        // Remove eventId from contextPath and request path.
        this.contextPath = PathTransformer.removeEventIdFromPath(eventId, contextPath);
        this.path = PathTransformer.removeEventIdFromPath(eventId, super.path());
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String contextPath() {
        return this.contextPath;
    }
}
