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
package io.gravitee.gateway.services.healthcheck.reporter;

import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StatusReporter implements Handler<EndpointStatus> {

    private final Logger logger = LoggerFactory.getLogger(StatusReporter.class);

    @Autowired
    private ReporterService reporterService;

    @Override
    public void handle(EndpointStatus edptStatus) {
        logger.debug("Report health results for {}", edptStatus.getApi());

        reporterService.report(edptStatus);
    }
}
