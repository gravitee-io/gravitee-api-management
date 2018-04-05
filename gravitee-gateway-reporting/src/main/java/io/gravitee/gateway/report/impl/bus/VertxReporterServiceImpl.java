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
package io.gravitee.gateway.report.impl.bus;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxReporterServiceImpl extends AbstractService implements ReporterService {

    private final static Logger LOGGER = LoggerFactory.getLogger(VertxReporterServiceImpl.class);

    @Autowired
    private Vertx vertx;

    private final Collection<Reporter> reporters = new ArrayList<>();

    private ReporterVerticle reporterVerticle;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        reporterVerticle = new ReporterVerticle();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(reporterVerticle);

        vertx.deployVerticle(reporterVerticle, event -> {
            if (event.failed()) {
                LOGGER.error("Reporter service can not be started", event.cause());
            }
        });

        if (! reporters.isEmpty()) {
            for (Reporter reporter : reporters) {
                try {
                    LOGGER.info("Starting reporter: {}", reporter);
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
    public void report(Reportable reportable) {
        // Is the verticle initialized ?
        if (reporterVerticle != null) {
            reporterVerticle.report(reportable);
        }
    }

    @Override
    public void register(Reporter reporter) {
        reporters.add(new EventBusReporterWrapper(vertx, reporter));
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (reporterVerticle.deploymentID() != null) {
            vertx.undeploy(reporterVerticle.deploymentID(), event -> {
                for(Reporter reporter: reporters) {
                    try {
                        LOGGER.info("Stopping reporter: {}", reporter);
                        reporter.stop();
                    } catch (Exception ex) {
                        LOGGER.error("Unexpected error while stopping reporter", ex);
                    }
                }
            });
        }
    }

    @Override
    protected String name() {
        return "Reporter service";
    }
}
