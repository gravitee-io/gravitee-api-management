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
package io.gravitee.gateway.services.monitoring;

import io.gravitee.common.node.Node;
import io.gravitee.gateway.core.reporter.ReporterService;
import io.gravitee.gateway.services.monitoring.probe.JvmProbe;
import io.gravitee.gateway.services.monitoring.probe.OsProbe;
import io.gravitee.gateway.services.monitoring.probe.ProcessProbe;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class MonitorThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorThread.class);

    @Autowired
    private ReporterService reporterService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private Node node;

    private Event heartbeatEvent;

    MonitorThread(Event heartbeatEvent) {
        this.heartbeatEvent = heartbeatEvent;
    }

    @Override
    public void run() {
        LOGGER.debug("Run monitor for gateway at {}", new Date());

        try {
            // Update heartbeat timestamp
            heartbeatEvent.setUpdatedAt(new Date());
            heartbeatEvent.getProperties().put("last_heartbeat_at",
                    Long.toString(heartbeatEvent.getUpdatedAt().getTime()));
            LOGGER.debug("Sending an heartbeat event");
            eventRepository.update(heartbeatEvent);

            // And generate monitoring metrics
            reporterService.report(
                    Monitor
                    .on(node.id())
                    .at(System.currentTimeMillis())
                    .os(OsProbe.getInstance().osInfo())
                    .jvm(JvmProbe.getInstance().jvmInfo())
                    .process(ProcessProbe.getInstance().processInfo())
                    .build());
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while monitoring the gateway", ex);
        }
    }
}