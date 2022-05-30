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

import static java.util.Collections.singleton;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.LoggingContent;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.LoggingScope;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.DebugApiService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DebugApiServiceImpl implements DebugApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugApiServiceImpl.class);
    private final ApiService apiService;
    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final InstanceService instanceService;
    private final PlanConverter planConverter;

    public DebugApiServiceImpl(
        ApiService apiService,
        EventService eventService,
        ObjectMapper objectMapper,
        InstanceService instanceService,
        PlanConverter planConverter
    ) {
        this.apiService = apiService;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.instanceService = instanceService;
        this.planConverter = planConverter;
    }

    @Override
    public EventEntity debug(final ExecutionContext executionContext, String apiId, String userId, DebugApiEntity debugApiEntity) {
        LOGGER.debug("Debug API : {}", apiId);

        apiService.checkPolicyConfigurations(debugApiEntity.getPaths(), debugApiEntity.getFlows(), debugApiEntity.getPlans());

        final ApiEntity api = apiService.findById(executionContext, apiId);

        final InstanceEntity instanceEntity = selectTargetGateway(executionContext, api);

        Map<String, String> properties = Map.ofEntries(
            entry(Event.EventProperties.USER.getValue(), userId),
            entry(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name()),
            entry(Event.EventProperties.GATEWAY_ID.getValue(), instanceEntity.getId())
        );

        DebugApi debugApi = convert(debugApiEntity, apiId);

        validatePlan(debugApi);
        validateDefinitionVersion(apiId, debugApi);

        return eventService.createDebugApiEvent(
            executionContext,
            singleton(executionContext.getEnvironmentId()),
            EventType.DEBUG_API,
            debugApi,
            properties
        );
    }

    private void validateDefinitionVersion(String apiId, DebugApi debugApi) {
        if (!debugApi.getDefinitionVersion().equals(DefinitionVersion.V2)) {
            throw new DebugApiInvalidDefinitionVersionException(apiId);
        }
    }

    private void validatePlan(DebugApi debugApi) {
        boolean hasValidPlan = debugApi
            .getPlans()
            .stream()
            .anyMatch(
                plan ->
                    PlanStatus.STAGING.name().equalsIgnoreCase(plan.getStatus()) ||
                    PlanStatus.PUBLISHED.name().equalsIgnoreCase(plan.getStatus())
            );

        if (!hasValidPlan) {
            throw new DebugApiNoValidPlanException(debugApi.getId());
        }
    }

    private InstanceEntity selectTargetGateway(ExecutionContext executionContext, ApiEntity api) {
        final List<InstanceEntity> startedInstances = instanceService.findAllStarted(executionContext);

        String debugPluginId = "gateway-debug";

        return startedInstances
            .stream()
            .filter(instanceEntity -> instanceEntity.getEnvironments().contains(api.getReferenceId()))
            .filter(
                instanceEntity -> instanceEntity.getPlugins().stream().map(PluginEntity::getId).anyMatch(debugPluginId::equalsIgnoreCase)
            )
            .filter(instanceEntity -> EnvironmentUtils.hasMatchingTags(ofNullable(instanceEntity.getTags()), api.getTags()))
            .max(Comparator.comparing(InstanceEntity::getStartedAt))
            .orElseThrow(() -> new DebugApiNoCompatibleInstanceException(api.getId()));
    }

    private DebugApi convert(DebugApiEntity debugApiEntity, String apiId) {
        DebugApi debugApi = new DebugApi();
        debugApi.setId(apiId);
        debugApi.setRequest(debugApiEntity.getRequest());
        debugApi.setResponse(debugApiEntity.getResponse());
        debugApi.setDefinitionVersion(DefinitionVersion.valueOfLabel(debugApiEntity.getGraviteeDefinitionVersion()));
        debugApi.setName(debugApiEntity.getName());
        debugApi.setVersion(debugApiEntity.getVersion());
        debugApi.setProperties(debugApiEntity.getProperties());
        debugApi.setProxy(debugApiEntity.getProxy());
        debugApi.setResources(debugApiEntity.getResources());
        debugApi.setResponseTemplates(debugApiEntity.getResponseTemplates());
        debugApi.setServices(debugApiEntity.getServices());
        debugApi.setTags(debugApiEntity.getTags());
        debugApi.setPaths(debugApiEntity.getPaths());
        debugApi.setFlows(debugApiEntity.getFlows());
        debugApi.setPlans(planConverter.toPlansDefinitions(debugApiEntity.getPlans()));

        // Disable logging for the debugged API
        if (debugApiEntity.getProxy() != null && debugApiEntity.getProxy().getLogging() != null) {
            debugApi.getProxy().getLogging().setMode(LoggingMode.NONE);
            debugApi.getProxy().getLogging().setContent(LoggingContent.NONE);
            debugApi.getProxy().getLogging().setScope(LoggingScope.NONE);
        }

        // Disable health-check for the debugger API
        if (debugApiEntity.getServices() != null && debugApi.getServices().getHealthCheckService() != null) {
            debugApi.getServices().getHealthCheckService().setEnabled(false);
        }

        return debugApi;
    }
}
