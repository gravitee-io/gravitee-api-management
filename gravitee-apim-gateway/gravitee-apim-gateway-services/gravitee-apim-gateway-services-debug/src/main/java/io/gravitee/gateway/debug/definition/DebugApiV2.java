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
import io.gravitee.gateway.debug.reactor.handler.context.PathTransformer;
import io.gravitee.gateway.handlers.api.definition.Api;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DebugApiV2 extends Api {

    private HttpRequest request;
    private HttpResponse response;
    private String eventId;

    public DebugApiV2(String eventId, io.gravitee.definition.model.debug.DebugApiV2 debugApi) {
        super(debugApi);
        this.setResponse(debugApi.getResponse());
        this.setRequest(debugApi.getRequest());
        this.setEventId(eventId);
        this.setEnabled(true);
    }

    /**
     * When setting eventId, virtual hosts' path are overriden  by /{eventId}-path in order to distinguish each instance
     * of debug api
     * @param eventId
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;

        // Instead of doing it here, we could implement a custom DebugHandlerEntrypointFactory which allows to override
        // path(), create a new virtual host with this new path to accept an overridden request targeting this path.
        if (definition.getProxy() != null && definition.getProxy().getVirtualHosts() != null) {
            definition
                .getProxy()
                .getVirtualHosts()
                .forEach(virtualHost -> virtualHost.setPath(PathTransformer.computePathWithEventId(this.eventId, virtualHost.getPath())));
        }
    }
}
