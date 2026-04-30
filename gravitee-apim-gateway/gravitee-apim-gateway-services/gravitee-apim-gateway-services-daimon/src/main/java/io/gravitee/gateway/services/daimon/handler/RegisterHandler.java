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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.services.daimon.DaimonRegistry;
import io.gravitee.gateway.services.daimon.DaimonRegistry.DeviceInfo;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class RegisterHandler implements Handler<RoutingContext> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DaimonRegistry registry;

    @Override
    public void handle(RoutingContext ctx) {
        ctx
            .request()
            .bodyHandler(body -> {
                try {
                    DeviceInfo device = MAPPER.readValue(body.getBytes(), DeviceInfo.class);
                    registry.register(device);

                    log.info("DAImon registered: {} ({}) from {}", device.getDeviceId(), device.getHostname(), device.getUser());

                    JsonObject response = new JsonObject()
                        .put("status", "registered")
                        .put("configVersion", 1)
                        .put("heartbeatIntervalSec", 30);

                    ctx
                        .response()
                        .setStatusCode(HttpStatusCode.OK_200)
                        .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .end(response.encodePrettily());
                } catch (Exception e) {
                    log.error("Failed to parse register request", e);
                    ctx
                        .response()
                        .setStatusCode(HttpStatusCode.BAD_REQUEST_400)
                        .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .end(new JsonObject().put("error", e.getMessage()).encode());
                }
            });
    }
}
