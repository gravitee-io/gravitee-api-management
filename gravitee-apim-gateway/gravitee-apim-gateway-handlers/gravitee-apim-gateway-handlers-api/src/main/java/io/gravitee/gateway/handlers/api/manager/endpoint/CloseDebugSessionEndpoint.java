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
package io.gravitee.gateway.handlers.api.manager.endpoint;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.core.v4.analytics.DebugSessionRegistry;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Closes a debug session early. Sessions expire on their own, so this only spares
 * the remaining window once an investigation is over.
 */
@CustomLog
public class CloseDebugSessionEndpoint implements Handler<RoutingContext>, ManagementEndpoint {

    @Autowired
    private DebugSessionRegistry debugSessionRegistry;

    @Override
    public HttpMethod method() {
        return HttpMethod.DELETE;
    }

    @Override
    public String path() {
        return "/debug-sessions/:apiId";
    }

    @Override
    public void handle(final RoutingContext ctx) {
        final String apiId = ctx.request().getParam("apiId");
        debugSessionRegistry.close(apiId);
        log.info("Debug session closed on API {}", apiId);
        ctx.response().setStatusCode(HttpStatusCode.NO_CONTENT_204).end();
    }
}
