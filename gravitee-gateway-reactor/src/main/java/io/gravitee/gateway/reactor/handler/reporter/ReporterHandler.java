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
package io.gravitee.gateway.reactor.handler.reporter;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.report.ReporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ReporterHandler implements BiConsumer<Response, Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporterHandler.class);

    private final ReporterService reporterService;
    private final Request serverRequest;

    public ReporterHandler(ReporterService reporterService, final Request serverRequest) {
        this.reporterService = reporterService;
        this.serverRequest = serverRequest;
    }

    @Override
    public void accept(Response response, Throwable throwable) {
        try {
            reporterService.report(serverRequest.metrics());
        } catch (Exception ex) {
            LOGGER.error("An error occurs while reporting metrics", ex);
        }
    }
}
