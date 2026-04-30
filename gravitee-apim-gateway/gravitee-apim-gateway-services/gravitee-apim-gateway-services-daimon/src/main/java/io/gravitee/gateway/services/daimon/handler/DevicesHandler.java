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
package io.gravitee.gateway.services.daimon.handler;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.services.daimon.DaimonRegistry;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class DevicesHandler implements Handler<RoutingContext> {

    private final DaimonRegistry registry;

    @Override
    public void handle(RoutingContext ctx) {
        JsonArray devices = new JsonArray();
        for (DaimonRegistry.DeviceInfo device : registry.allDevices()) {
            devices.add(
                new JsonObject()
                    .put("deviceId", device.getDeviceId())
                    .put("hostname", device.getHostname())
                    .put("user", device.getUser())
                    .put("version", device.getVersion())
                    .put("os", device.getOs())
                    .put("registeredAt", device.getRegisteredAt() != null ? device.getRegisteredAt().toString() : null)
                    .put("lastSeen", device.getLastSeen() != null ? device.getLastSeen().toString() : null)
            );
        }
        ctx
            .response()
            .setStatusCode(HttpStatusCode.OK_200)
            .putHeader("Content-Type", MediaType.APPLICATION_JSON)
            .end(devices.encodePrettily());
    }
}
