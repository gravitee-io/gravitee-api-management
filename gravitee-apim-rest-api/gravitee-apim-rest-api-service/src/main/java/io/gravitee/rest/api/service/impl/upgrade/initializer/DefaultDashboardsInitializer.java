/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.DashboardReferenceType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultDashboardsInitializer implements Initializer {

    private final DashboardService dashboardService;

    @Lazy
    private final EnvironmentService environmentService;

    @Override
    public boolean initialize() {
        final List<DashboardEntity> dashboards = dashboardService.findAll();

        findAllEnvironments()
            .stream()
            .filter(environment -> shouldConfigureDashboardForEnvironment(environment, dashboards))
            .map(environment -> new ExecutionContext(environment.getOrganizationId(), environment.getId()))
            .forEach(dashboardService::initialize);

        return true;
    }

    private Set<EnvironmentEntity> findAllEnvironments() {
        try {
            return environmentService.findAllOrInitialize();
        } catch (TechnicalManagementException e) {
            log.warn("An error occurs searching all environments.");
        }
        return Set.of();
    }

    /**
     * Checks if dashboards have been initialized for the environment
     * @param environment is the reference to the environment to check if there are configured dashboards
     * @param dashboards is the list of configured dashboards in database
     * @return true if dashboards need to be initialized for the environment.
     */
    private static boolean shouldConfigureDashboardForEnvironment(EnvironmentEntity environment, List<DashboardEntity> dashboards) {
        return dashboards
            .stream()
            .noneMatch(
                dashboard ->
                    dashboard.getReferenceType().equals(DashboardReferenceType.ENVIRONMENT.name()) &&
                    dashboard.getReferenceId().equals(environment.getId())
            );
    }

    @Override
    public int getOrder() {
        return InitializerOrder.DEFAULT_DASHBOARDS_INITIALIZER;
    }
}
