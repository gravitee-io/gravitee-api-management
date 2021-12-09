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
package io.gravitee.gateway.reactor.processor.notfound;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;

public class NotFoundReporter extends AbstractProcessor<ExecutionContext> {

    private static final String UNKNOWN_SERVICE = "1";

    private final ReporterService reporterService;
    private final boolean logEnabled;

    public NotFoundReporter(final ReporterService reporterService, final boolean logEnabled) {
        this.reporterService = reporterService;
        this.logEnabled = logEnabled;
    }

    @Override
    public void handle(ExecutionContext context) {
        Metrics metrics = context.request().metrics();
        metrics.setApi(UNKNOWN_SERVICE);
        metrics.setApplication(UNKNOWN_SERVICE);
        metrics.setPath(context.request().pathInfo());

        if (logEnabled) {
            Buffer payload = Buffer.buffer();

            context
                .request()
                .bodyHandler(payload::appendBuffer)
                .endHandler(
                    aVoid -> {
                        Log log = new Log(System.currentTimeMillis());
                        log.setRequestId(context.request().id());
                        log.setClientRequest(new io.gravitee.reporter.api.common.Request());
                        log.getClientRequest().setMethod(context.request().method());
                        log.getClientRequest().setUri(context.request().uri());
                        log.getClientRequest().setHeaders(context.request().headers());
                        log.getClientRequest().setBody(payload.toString());

                        metrics.setLog(log);

                        reporterService.report(metrics);
                        next.handle(context);
                    }
                );
        } else {
            reporterService.report(metrics);
            next.handle(context);
        }
    }
}
