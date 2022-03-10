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
package io.gravitee.gateway.standalone.reporter;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeReporter extends AbstractService implements Reporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeReporter.class);

    static final Handler<Reportable> DEFAULT_REPORT_HANDLER = reportable -> LOGGER.debug("Reporting: {}", reportable.toString());

    private Handler<Reportable> reportHandler = DEFAULT_REPORT_HANDLER;

    public void setReportableHandler(Handler<Reportable> reportHandler) {
        this.reportHandler = reportHandler;
    }

    @Override
    public void report(Reportable reportable) {
        reportHandler.handle(reportable);
    }

    public void reset() {
        reportHandler = DEFAULT_REPORT_HANDLER;
    }
}
