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
package io.gravitee.rest.api.service.impl.upgrade;

import static io.gravitee.repository.management.model.DashboardReferenceType.*;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;

import io.gravitee.repository.management.model.DashboardReferenceType;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.Upgrader;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultDashboardsUpgrader implements Upgrader, Ordered {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultDashboardsUpgrader.class);

    @Autowired
    private DashboardService dashboardService;

    @Override
    public boolean upgrade() {
        final List<DashboardEntity> dashboards = dashboardService.findAll();
        if (dashboards == null || dashboards.isEmpty()) {
            checkAndCreateDashboard(PLATFORM);
            checkAndCreateDashboard(API);
            checkAndCreateDashboard(APPLICATION);
        }
        return true;
    }

    private void checkAndCreateDashboard(final DashboardReferenceType referenceType) {
        LOGGER.info("    No default {}'s dashboards, creating default ones...", referenceType);
        createDashboard(referenceType, "Global");
        createDashboard(referenceType, "Geo");
        createDashboard(referenceType, "User");
        LOGGER.info("    Added default {}'s dashboards with success", referenceType);
    }

    private void createDashboard(final DashboardReferenceType referenceType, final String prefixName) {
        final NewDashboardEntity dashboard = new NewDashboardEntity();
        dashboard.setName(prefixName + " dashboard");
        dashboard.setReferenceId("DEFAULT");
        dashboard.setReferenceType(io.gravitee.rest.api.model.DashboardReferenceType.valueOf(referenceType.name()));
        dashboard.setEnabled(true);
        final String filePath = format("/dashboards/%s_%s.json", referenceType.name().toLowerCase(), prefixName.toLowerCase());
        try {
            dashboard.setDefinition(IOUtils.toString(this.getClass().getResourceAsStream(filePath), defaultCharset()));
        } catch (final Exception e) {
            LOGGER.error("Error while trying to create a dashboard from the definition path: " + filePath, e);
        }
        dashboardService.create(dashboard);
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
