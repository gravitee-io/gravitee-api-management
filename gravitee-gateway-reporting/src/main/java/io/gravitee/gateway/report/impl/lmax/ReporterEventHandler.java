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
package io.gravitee.gateway.report.impl.lmax;

import com.lmax.disruptor.EventHandler;
import io.gravitee.reporter.api.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class ReporterEventHandler implements EventHandler<ReportableEvent> {

    private final Logger logger = LoggerFactory.getLogger(ReporterEventHandler.class);

    private final Reporter reporter;

    ReporterEventHandler(final Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void onEvent(ReportableEvent event, long l, boolean b) throws Exception {
        if (reporter.canHandle(event.getReportable())) {
            try {
                reporter.report(event.getReportable());
            } catch (Exception ex) {
                logger.error("Unexpected error while reporting event {}", event.getReportable(), ex);
            }
        }
    }
}
