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
package io.gravitee.gateway.services.sync.process.handler;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.services.sync.process.DefaultSyncManager;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DefaultSyncHandler implements Handler<RoutingContext> {

    private final DefaultSyncManager defaultSyncManager;

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        JsonObject object = new JsonObject()
            .put("initialDone", defaultSyncManager.syncDone())
            .put("counter", defaultSyncManager.syncCounter())
            .put("nextSyncTime", defaultSyncManager.nextSyncTime())
            .put("lastOnError", defaultSyncManager.lastSyncOnError())
            .put("lastErrorMessage", defaultSyncManager.lastSyncErrorMessage())
            .put("totalOnErrors", defaultSyncManager.totalSyncOnError());

        response.setStatusCode(defaultSyncManager.syncDone() ? HttpStatusCode.OK_200 : HttpStatusCode.SERVICE_UNAVAILABLE_503);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        response.setChunked(true);

        response.write(object.encodePrettily());
        response.end();
    }
}
