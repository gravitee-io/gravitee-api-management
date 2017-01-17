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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.analytics.HealthAnalytics;
import io.gravitee.management.service.HealthCheckService;
import io.gravitee.repository.healthcheck.HealthCheckRepository;
import io.gravitee.repository.healthcheck.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HealthCheckServiceImpl implements HealthCheckService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    @Autowired
    private HealthCheckRepository healthCheckRepository;

    @Override
    public HealthAnalytics health(String api, long from, long to, long interval) {
        logger.debug("Run health query for API '{}'", api);

        try {
            return convert(healthCheckRepository.query(api, interval, from, to));
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for health data.", ex);
            return null;
        }
    }

    private HealthAnalytics convert(HealthResponse response) {
        HealthAnalytics healthAnalytics = new HealthAnalytics();

        healthAnalytics.setTimestamps(response.timestamps());
        healthAnalytics.setBuckets(response.buckets());

        return healthAnalytics;
    }
}
