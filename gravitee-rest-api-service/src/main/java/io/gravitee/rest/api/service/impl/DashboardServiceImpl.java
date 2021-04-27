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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.DASHBOARD;
import static io.gravitee.repository.management.model.Dashboard.AuditEvent.*;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.management.model.DashboardReferenceType;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.model.UpdateDashboardEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.DashboardNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DashboardServiceImpl extends AbstractService implements DashboardService {

    private final Logger LOGGER = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final String DEFAULT_HOME_DASHBOARD_PATH = "/home.json";

    @Value("${console.dashboards.path:${gravitee.home}/dashboards}")
    private String dashboardsPath;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public List<DashboardEntity> findAll() {
        try {
            LOGGER.debug("Find all dashboards");
            return dashboardRepository
                .findAll()
                .stream()
                .map(this::convert)
                .sorted(comparingInt(DashboardEntity::getOrder))
                .sorted(comparing(DashboardEntity::getReferenceType))
                .collect(toList());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find all dashboards";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public List<DashboardEntity> findByReferenceType(final DashboardReferenceType referenceType) {
        try {
            LOGGER.debug("Find all dashboards by reference type");
            if (DashboardReferenceType.HOME.equals(referenceType)) {
                DashboardEntity dashboard = new DashboardEntity();
                dashboard.setDefinition(this.getHomeDashboardDefinition());
                dashboard.setReferenceType(referenceType.name());
                dashboard.setEnabled(true);
                dashboard.setName("Home dashboard");
                dashboard.setReferenceId("DEFAULT");
                return Collections.singletonList(dashboard);
            } else {
                return dashboardRepository.findByReferenceType(referenceType.name()).stream().map(this::convert).collect(toList());
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find all dashboards by reference type";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public DashboardEntity findById(String dashboardId) {
        try {
            LOGGER.debug("Find dashboard by ID: {}", dashboardId);
            Optional<Dashboard> optDashboard = dashboardRepository.findById(dashboardId);
            if (!optDashboard.isPresent()) {
                throw new DashboardNotFoundException(dashboardId);
            }
            return convert(optDashboard.get());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find dashboard by ID";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public DashboardEntity create(final NewDashboardEntity dashboardEntity) {
        try {
            final List<Dashboard> dashboards = dashboardRepository.findByReferenceType(dashboardEntity.getReferenceType().name());
            Dashboard dashboard = dashboardRepository.create(convert(dashboardEntity, dashboards));
            auditService.createEnvironmentAuditLog(
                Collections.singletonMap(DASHBOARD, dashboard.getId()),
                DASHBOARD_CREATED,
                dashboard.getCreatedAt(),
                null,
                dashboard
            );
            return convert(dashboard);
        } catch (TechnicalException ex) {
            final String error = "An error occurred while trying to create dashboard " + dashboardEntity;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public DashboardEntity update(UpdateDashboardEntity dashboardEntity) {
        try {
            final Optional<Dashboard> dashboardOptional = dashboardRepository.findById(dashboardEntity.getId());
            if (dashboardOptional.isPresent()) {
                final Dashboard dashboard = convert(dashboardEntity);
                final DashboardEntity savedDashboard;
                if (dashboard.getOrder() != dashboardOptional.get().getOrder()) {
                    savedDashboard = reorderAndSaveDashboards(dashboard, false);
                } else {
                    savedDashboard = convert(dashboardRepository.update(dashboard));
                }
                auditService.createEnvironmentAuditLog(
                    Collections.singletonMap(DASHBOARD, dashboard.getId()),
                    DASHBOARD_UPDATED,
                    new Date(),
                    dashboardOptional.get(),
                    dashboard
                );
                return savedDashboard;
            } else {
                throw new DashboardNotFoundException(dashboardEntity.getId());
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurred while trying to update dashboard " + dashboardEntity;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private String getHomeDashboardDefinition() {
        return this.getDefinition(dashboardsPath + DEFAULT_HOME_DASHBOARD_PATH);
    }

    private String getDefinition(String path) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = new String(Files.readAllBytes(new File(path).toPath()), defaultCharset());
            // Important for remove formatting (space, line break...)
            JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
            return jsonNode.toString();
        } catch (IOException ex) {
            final String error = "Error while trying to load a dashboard from the definition path: " + path;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private DashboardEntity reorderAndSaveDashboards(final Dashboard dashboardToReorder, final boolean deleted) throws TechnicalException {
        final Collection<Dashboard> dashboards = dashboardRepository.findByReferenceType(dashboardToReorder.getReferenceType());
        final Dashboard[] dashboardsToReorder = dashboards
            .stream()
            .filter(d -> !Objects.equals(d.getId(), dashboardToReorder.getId()))
            .toArray(Dashboard[]::new);
        if (dashboardToReorder.getOrder() < 1) {
            dashboardToReorder.setOrder(1);
        } else if (dashboardToReorder.getOrder() > dashboardsToReorder.length + 1) {
            dashboardToReorder.setOrder(dashboardsToReorder.length + 1);
        }
        try {
            for (int i = 0; i < dashboardsToReorder.length; i++) {
                if (deleted) {
                    if (dashboardsToReorder[i].getOrder() > dashboardToReorder.getOrder()) {
                        dashboardsToReorder[i].setOrder(dashboardsToReorder[i].getOrder() - 1);
                        dashboardRepository.update(dashboardsToReorder[i]);
                    }
                } else {
                    int newOrder = (i + 1) < dashboardToReorder.getOrder() ? (i + 1) : (i + 2);
                    if (dashboardsToReorder[i].getOrder() != newOrder) {
                        dashboardsToReorder[i].setOrder(newOrder);
                        dashboardRepository.update(dashboardsToReorder[i]);
                    }
                }
            }
            if (deleted) {
                return null;
            }
            return convert(dashboardRepository.update(dashboardToReorder));
        } catch (final TechnicalException ex) {
            final String error = "An error occurs while trying to update dashboard " + dashboardToReorder.getId();
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public void delete(final String dashboardId) {
        try {
            Optional<Dashboard> dashboardOptional = dashboardRepository.findById(dashboardId);
            if (dashboardOptional.isPresent()) {
                dashboardRepository.delete(dashboardId);
                reorderAndSaveDashboards(dashboardOptional.get(), true);
                auditService.createEnvironmentAuditLog(
                    Collections.singletonMap(DASHBOARD, dashboardId),
                    DASHBOARD_DELETED,
                    new Date(),
                    null,
                    dashboardOptional.get()
                );
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to delete dashboard " + dashboardId;
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private Dashboard convert(final NewDashboardEntity dashboardEntity, final List<Dashboard> dashboards) {
        final Dashboard dashboard = new Dashboard();
        dashboard.setId(RandomString.generate());
        dashboard.setReferenceId(dashboardEntity.getReferenceId());
        dashboard.setReferenceType(dashboardEntity.getReferenceType().name());
        dashboard.setName(dashboardEntity.getName());
        dashboard.setQueryFilter(dashboardEntity.getQueryFilter());
        dashboard.setEnabled(dashboardEntity.isEnabled());
        dashboard.setDefinition(dashboardEntity.getDefinition());
        if (dashboards == null || dashboards.isEmpty()) {
            dashboard.setOrder(1);
        } else {
            dashboard.setOrder(dashboards.stream().max(comparingInt(Dashboard::getOrder)).map(Dashboard::getOrder).get() + 1);
        }
        final Date now = new Date();
        dashboard.setCreatedAt(now);
        dashboard.setUpdatedAt(now);
        return dashboard;
    }

    private Dashboard convert(final UpdateDashboardEntity dashboardEntity) {
        final Dashboard dashboard = new Dashboard();
        dashboard.setId(dashboardEntity.getId());
        dashboard.setReferenceId(dashboardEntity.getReferenceId());
        dashboard.setReferenceType(dashboardEntity.getReferenceType().name());
        dashboard.setName(dashboardEntity.getName());
        dashboard.setQueryFilter(dashboardEntity.getQueryFilter());
        dashboard.setOrder(dashboardEntity.getOrder());
        dashboard.setEnabled(dashboardEntity.isEnabled());
        dashboard.setDefinition(dashboardEntity.getDefinition());
        dashboard.setUpdatedAt(new Date());
        return dashboard;
    }

    private DashboardEntity convert(final Dashboard dashboard) {
        final DashboardEntity dashboardEntity = new DashboardEntity();
        dashboardEntity.setId(dashboard.getId());
        dashboardEntity.setReferenceId(dashboard.getReferenceId());
        dashboardEntity.setReferenceType(dashboard.getReferenceType());
        dashboardEntity.setName(dashboard.getName());
        dashboardEntity.setQueryFilter(dashboard.getQueryFilter());
        dashboardEntity.setOrder(dashboard.getOrder());
        dashboardEntity.setEnabled(dashboard.isEnabled());
        dashboardEntity.setDefinition(dashboard.getDefinition());
        dashboardEntity.setCreatedAt(dashboard.getCreatedAt());
        dashboardEntity.setUpdatedAt(dashboard.getUpdatedAt());
        return dashboardEntity;
    }
}
