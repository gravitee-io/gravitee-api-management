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
package io.gravitee.gateway.report.impl;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReporterServiceImpl extends AbstractService implements ReporterService {

    private final Logger LOGGER = LoggerFactory.getLogger(ReporterServiceImpl.class);

    private final Collection<Reporter> reporters = new ArrayList<>();

    @Override
    public void register(Reporter reporter) {
        reporters.add(reporter);
    }

    @Override
    public void report(Reportable reportable) {
        doReport(reportable);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        if (! reporters.isEmpty()) {
            for (Reporter reporter : reporters) {
                try {
                    reporter.start();
                } catch (Exception ex) {
                    LOGGER.error("Unexpected error while starting reporter", ex);
                }
            }
        } else {
            LOGGER.info("\tThere is no reporter to start");
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        for(Reporter reporter: reporters) {
            try {
                reporter.stop();
            } catch (Exception ex) {
                LOGGER.error("Unexpected error while starting reporter", ex);
            }
        }
    }

    protected Collection<Reporter> getReporters() {
        return reporters;
    }

    protected void doReport(Reportable reportable) {
        for (Reporter reporter : reporters) {
            if (reporter.canHandle(reportable)) {
                reporter.report(reportable);
            }
        }
    }
}
