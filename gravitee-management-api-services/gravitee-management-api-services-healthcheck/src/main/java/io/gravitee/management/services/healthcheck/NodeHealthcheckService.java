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
package io.gravitee.management.services.healthcheck;

import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.management.services.healthcheck.handler.HealthcheckHandler;
import io.gravitee.management.services.healthcheck.probe.AnalyticsRepositoryProbe;
import io.gravitee.management.services.healthcheck.probe.ManagementApiProbe;
import io.gravitee.management.services.healthcheck.probe.ManagementRepositoryProbe;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeHealthcheckService extends AbstractService {

    private final Logger logger = LoggerFactory.getLogger(NodeHealthcheckService.class);

    private final static Collection<Class<? extends Probe>> PROBES = Arrays.asList(
            ManagementApiProbe.class, ManagementRepositoryProbe.class, AnalyticsRepositoryProbe.class);

    private final static String PATH = "/health";

    @Autowired
    private Router router;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        logger.info("Associate a new HTTP handler on {}", PATH);

        // Create handler
        Handler<RoutingContext> healthHandler = createHandler();

        // Associate a new handler
        router.get(PATH).produces(MediaType.APPLICATION_JSON).handler(healthHandler);
    }

    private Handler<RoutingContext> createHandler() {
        HealthcheckHandler handler = new HealthcheckHandler();

        List<Probe> probes = new ArrayList<>();
        for(Class<? extends Probe> clazz : PROBES) {
            Probe probe = createProbe(clazz);
            if (probe != null) {
                probes.add(probe);
            }
        }

        handler.setProbes(probes);
        return handler;
    }

    private Probe createProbe(Class<? extends Probe> probeClass) {
        Probe probe = null;
        try {
            probe = probeClass.newInstance();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(probe);
        } catch (Exception ex) {
            logger.error("Unexpected error while creating an health-check probe for {}", probeClass.getName(), ex);
        }

        return probe;
    }

    @Override
    protected String name() {
        return "Node Health-check service";
    }
}
