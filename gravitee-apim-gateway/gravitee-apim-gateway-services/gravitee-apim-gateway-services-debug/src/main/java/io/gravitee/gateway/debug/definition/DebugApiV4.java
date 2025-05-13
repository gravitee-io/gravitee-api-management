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
package io.gravitee.gateway.debug.definition;

import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.gateway.debug.reactor.handler.context.PathTransformer;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DebugApiV4 extends Api implements ReactableDebugApi<io.gravitee.definition.model.v4.Api> {

    private HttpRequest request;
    private HttpResponse response;
    private String eventId;

    public DebugApiV4(String eventId, io.gravitee.definition.model.debug.DebugApiV4 debugApi) {
        super(debugApi.getApiDefinition());
        this.setRequest(debugApi.getRequest());
        this.setResponse(debugApi.getResponse());
        this.setEventId(eventId);
        this.setEnabled(true);
    }

    /**
     * When setting eventId, virtual hosts' path are overriden  by /{eventId}-path in order to distinguish each instance
     * of debug api
     * @param eventId The eventId
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;

        if (definition.getListeners() != null) {
            definition
                .getListeners()
                .stream()
                .filter(l -> l.getType() == io.gravitee.definition.model.v4.listener.ListenerType.HTTP)
                .forEach(listener -> {
                    HttpListener httpListener = (HttpListener) listener;
                    httpListener
                        .getPaths()
                        .forEach(path -> path.setPath(PathTransformer.computePathWithEventId(this.eventId, path.getPath())));
                });
        }
    }

    @Override
    public String extractHost() {
        return definition
            .getListeners()
            .stream()
            .filter(l -> l.getType() == io.gravitee.definition.model.v4.listener.ListenerType.HTTP)
            .flatMap(listener -> ((HttpListener) listener).getPaths().stream())
            .findFirst()
            .map(Path::getHost)
            .orElse(null);
    }

    @Override
    public String extractUri() {
        return (
            definition
                .getListeners()
                .stream()
                .filter(l -> l.getType() == io.gravitee.definition.model.v4.listener.ListenerType.HTTP)
                .flatMap(listener -> ((HttpListener) listener).getPaths().stream())
                .map(Path::getPath)
                .findFirst()
                .map(path -> path.endsWith("/") ? path.substring(0, path.length() - 1) : path)
                .orElse("") +
            request.getPath()
        );
    }
}
