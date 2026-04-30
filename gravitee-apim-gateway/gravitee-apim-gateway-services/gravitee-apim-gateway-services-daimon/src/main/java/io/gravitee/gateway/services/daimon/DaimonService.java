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
package io.gravitee.gateway.services.daimon;

import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.services.daimon.handler.DevicesHandler;
import io.gravitee.gateway.services.daimon.handler.HeartbeatHandler;
import io.gravitee.gateway.services.daimon.handler.RegisterHandler;
import io.vertx.ext.web.Router;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class DaimonService extends AbstractService<DaimonService> {

    private static final String REGISTER_PATH = "/daimon/register";
    private static final String HEARTBEAT_PATH = "/daimon/heartbeat";
    private static final String DEVICES_PATH = "/daimon/devices";

    private final Router router;
    private final DaimonRegistry registry;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        log.info("Starting DAImon control service");

        router
            .post(REGISTER_PATH)
            .consumes(MediaType.APPLICATION_JSON)
            .produces(MediaType.APPLICATION_JSON)
            .handler(new RegisterHandler(registry));

        router
            .post(HEARTBEAT_PATH)
            .consumes(MediaType.APPLICATION_JSON)
            .produces(MediaType.APPLICATION_JSON)
            .handler(new HeartbeatHandler(registry));

        router.get(DEVICES_PATH).produces(MediaType.APPLICATION_JSON).handler(new DevicesHandler(registry));

        log.info("DAImon control service started on {}, {}, {}", REGISTER_PATH, HEARTBEAT_PATH, DEVICES_PATH);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        log.info("Stopping DAImon control service");
    }
}
