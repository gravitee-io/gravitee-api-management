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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.DASHBOARD;
import static io.gravitee.repository.management.model.Dashboard.AuditEvent.*;
import static io.gravitee.repository.management.model.DashboardType.API;
import static io.gravitee.repository.management.model.DashboardType.APPLICATION;
import static io.gravitee.repository.management.model.DashboardType.PLATFORM;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.management.model.DashboardType;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.DashboardReferenceType;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.model.UpdateDashboardEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.DashboardNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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

    @Lazy
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
                .sorted(comparing(DashboardEntity::getType))
                .toList();
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find all dashboards";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public List<DashboardEntity> findAllByReference(DashboardReferenceType referenceType, String referenceId) {
        try {
            LOGGER.debug("Find all dashboards for {} - {}", referenceType, referenceId);
            return dashboardRepository
                .findByReference(referenceType.name(), referenceId)
                .stream()
                .map(this::convert)
                .sorted(comparingInt(DashboardEntity::getOrder))
                .sorted(comparing(DashboardEntity::getType))
                .toList();
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find all dashboards";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    public List<DashboardEntity> findByReferenceAndType(
        DashboardReferenceType referenceType,
        String referenceId,
        final DashboardType type
    ) {
        try {
            LOGGER.debug("Find all dashboards by reference type");
            if (DashboardType.HOME.equals(type)) {
                DashboardEntity dashboard = new DashboardEntity();
                dashboard.setDefinition(this.getHomeDashboardDefinition());
                dashboard.setReferenceType(referenceType.name());
                dashboard.setEnabled(true);
                dashboard.setName("Home dashboard");
                dashboard.setReferenceId(referenceId);
                return Collections.singletonList(dashboard);
            } else {
                return dashboardRepository
                    .findByReferenceAndType(referenceType.name(), referenceId, type.name())
                    .stream()
                    .map(this::convert)
                    .toList();
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find all dashboards by reference type";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public DashboardEntity findByReferenceAndId(DashboardReferenceType referenceType, String referenceId, String dashboardId) {
        try {
            LOGGER.debug("Find dashboard by ID: {}", dashboardId);
            return dashboardRepository
                .findByReferenceAndId(referenceType.name(), referenceId, dashboardId)
                .map(this::convert)
                .orElseThrow(() -> new DashboardNotFoundException(dashboardId));
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to find dashboard by ID";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public DashboardEntity create(ExecutionContext executionContext, final NewDashboardEntity dashboardEntity) {
        try {
            final List<Dashboard> dashboards = dashboardRepository.findByReferenceAndType(
                dashboardEntity.getReferenceType().name(),
                executionContext.getEnvironmentId(),
                dashboardEntity.getType().name()
            );
            Dashboard dashboard = dashboardRepository.create(convert(dashboardEntity, dashboards));
            auditService.createAuditLog(
                executionContext,
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
    public void initialize(ExecutionContext executionContext) {
        createDashboardsOfType(executionContext, PLATFORM);
        createDashboardsOfType(executionContext, API);
        createDashboardsOfType(executionContext, APPLICATION);
    }

    private void createDashboardsOfType(ExecutionContext executionContext, final DashboardType referenceType) {
        LOGGER.info(
            "    No default {}'s dashboards for environment {}, creating default ones...",
            referenceType,
            executionContext.getEnvironmentId()
        );
        createDashboard(executionContext, referenceType, "Global");
        createDashboard(executionContext, referenceType, "Geo");
        createDashboard(executionContext, referenceType, "User");
        LOGGER.info(
            "    Added default {}'s dashboards with success for environment {}",
            referenceType,
            executionContext.getEnvironmentId()
        );
    }

    private void createDashboard(ExecutionContext executionContext, DashboardType type, String prefixName) {
        final NewDashboardEntity dashboard = NewDashboardEntity.builder()
            .name(prefixName + " dashboard")
            .referenceType(DashboardReferenceType.ENVIRONMENT)
            .referenceId(executionContext.getEnvironmentId())
            .type(io.gravitee.rest.api.model.DashboardType.valueOf(type.name()))
            .enabled(true)
            .build();
        final String filePath = format("/dashboards/%s_%s.json", type.name().toLowerCase(), prefixName.toLowerCase());
        try {
            dashboard.setDefinition(IOUtils.toString(this.getClass().getResourceAsStream(filePath), defaultCharset()));
        } catch (final Exception e) {
            LOGGER.error("Error while trying to create a dashboard from the definition path: " + filePath, e);
        }
        this.create(executionContext, dashboard);
    }

    @Override
    public DashboardEntity update(
        ExecutionContext executionContext,
        DashboardReferenceType referenceType,
        String referenceId,
        UpdateDashboardEntity dashboardEntity
    ) {
        try {
            final Dashboard existing = dashboardRepository
                .findByReferenceAndId(referenceType.name(), referenceId, dashboardEntity.getId())
                .orElseThrow(() -> new DashboardNotFoundException(dashboardEntity.getId()));
            final Dashboard dashboard = convert(dashboardEntity);
            final DashboardEntity savedDashboard;
            if (dashboard.getOrder() != existing.getOrder()) {
                savedDashboard = reorderAndSaveDashboards(
                    dashboardEntity.getReferenceType(),
                    dashboardEntity.getReferenceId(),
                    dashboard,
                    false
                );
            } else {
                savedDashboard = convert(dashboardRepository.update(dashboard));
            }
            auditService.createAuditLog(
                executionContext,
                Collections.singletonMap(DASHBOARD, dashboard.getId()),
                DASHBOARD_UPDATED,
                new Date(),
                existing,
                dashboard
            );
            return savedDashboard;
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

    private DashboardEntity reorderAndSaveDashboards(
        final DashboardReferenceType dashboardReferenceType,
        final String referenceId,
        final Dashboard dashboardToReorder,
        final boolean deleted
    ) throws TechnicalException {
        final Collection<Dashboard> dashboards = dashboardRepository.findByReferenceAndType(
            dashboardReferenceType.name(),
            referenceId,
            dashboardToReorder.getReferenceType()
        );
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
    public void delete(
        ExecutionContext executionContext,
        DashboardReferenceType referenceType,
        String referenceId,
        final String dashboardId
    ) {
        try {
            Optional<Dashboard> dashboardOptional = dashboardRepository.findByReferenceAndId(
                referenceType.name(),
                referenceId,
                dashboardId
            );
            if (dashboardOptional.isPresent()) {
                dashboardRepository.delete(dashboardId);
                reorderAndSaveDashboards(referenceType, referenceId, dashboardOptional.get(), true);
                auditService.createAuditLog(
                    executionContext,
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
        dashboard.setId(UuidString.generateRandom());
        dashboard.setReferenceId(dashboardEntity.getReferenceId());
        dashboard.setReferenceType(dashboardEntity.getReferenceType().name());
        dashboard.setType(dashboardEntity.getType().name());
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
        dashboard.setType(dashboardEntity.getType().name());
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
        dashboardEntity.setType(dashboard.getType());
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
