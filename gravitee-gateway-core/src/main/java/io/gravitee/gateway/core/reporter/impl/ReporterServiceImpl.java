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
package io.gravitee.gateway.core.reporter.impl;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.metrics.Metrics;
import io.gravitee.gateway.api.reporter.MetricsReporter;
import io.gravitee.gateway.api.reporter.Reporter;
import io.gravitee.gateway.core.reporter.ReporterManager;
import io.gravitee.gateway.core.reporter.ReporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ReporterServiceImpl extends AbstractService implements ReporterService {

    private final Logger LOGGER = LoggerFactory.getLogger(ReporterServiceImpl.class);

    @Autowired
    private ReporterManager reporterManager;

    @Override
    public void report(Metrics metrics) {
        for(MetricsReporter reporter: reporterManager.getReporters()) {
            reporter.report(metrics);
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        if (! reporterManager.getReporters().isEmpty()) {
            for (Reporter reporter : reporterManager.getReporters()) {
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

        for(Reporter reporter: reporterManager.getReporters()) {
            try {
                reporter.stop();
            } catch (Exception ex) {
                LOGGER.error("Unexpected error while starting reporter", ex);
            }
        }
    }
}
