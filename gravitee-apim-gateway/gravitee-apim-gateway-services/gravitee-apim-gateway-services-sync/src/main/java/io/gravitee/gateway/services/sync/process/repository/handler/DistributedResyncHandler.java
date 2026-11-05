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
package io.gravitee.gateway.services.sync.process.repository.handler;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;

/**
 * Asks the distributed sync primary to rebuild the whole distributed catalog: every live entity is republished and
 * the entries that are no longer live are removed. Only the primary accepts the request.
 */
@RequiredArgsConstructor
public class DistributedResyncHandler implements Handler<RoutingContext> {

    private final DefaultSyncManager syncManager;

    @Override
    public void handle(RoutingContext ctx) {
        boolean accepted = syncManager.requestDistributedResync();
        JsonObject body = new JsonObject()
            .put("accepted", accepted)
            .put(
                "message",
                accepted
                    ? "The distributed sync catalog will be rebuilt on the next synchronization cycle"
                    : "Only the distributed sync primary node can rebuild the catalog, send the request to the primary node"
            );
        ctx
            .response()
            .setStatusCode(accepted ? HttpStatusCode.ACCEPTED_202 : HttpStatusCode.CONFLICT_409)
            .putHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .end(body.encodePrettily());
    }
}
