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
package io.gravitee.apim.core.debug.use_case;

import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiPolicyValidatorDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.debug.exceptions.DebugApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.debug.exceptions.DebugApiNoCompatibleInstanceException;
import io.gravitee.apim.core.debug.exceptions.DebugApiNoValidPlanException;
import io.gravitee.apim.core.debug.model.ApiDebugStatus;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.LoggingContent;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.LoggingScope;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PlanStatus;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class DebugApiUseCase {

    private final ApiPolicyValidatorDomainService apiPolicyValidatorDomainService;
    private final ApiCrudService apiCrudService;
    private final InstanceQueryService instanceQueryService;
    private final EventCrudService eventCrudService;

    public DebugApiUseCase.Output execute(DebugApiUseCase.Input input) throws InvalidPathsException {
        log.debug("Debugging API {}", input.apiId);

        validateDefinitionVersion(input.apiId, input.debugApi.getDefinitionVersion());
        validateApiExists(input);

        // Check policy configuration
        apiPolicyValidatorDomainService.checkPolicyConfigurations(input.debugApi, new HashSet<>(input.debugApi.getPlans()));

        final Instance selectedGateway = selectTargetGateway(
            input.auditInfo.organizationId(),
            input.auditInfo.environmentId(),
            input.debugApi
        );

        // prepare and validate debugApi
        validatePlan(input.debugApi);
        disableLoggingForDebug(input.debugApi);
        disableHealthCheckForDebug(input.debugApi);

        final Event debugApiEvent = createDebugApiEvent(input.auditInfo, input.debugApi, selectedGateway);

        return new DebugApiUseCase.Output(debugApiEvent);
    }

    private void validateApiExists(Input input) {
        if (!apiCrudService.existsById(input.apiId)) {
            throw new ApiNotFoundException(input.apiId);
        }
    }

    private Event createDebugApiEvent(AuditInfo auditInfo, DebugApi debugApi, Instance selectedInstance) {
        return eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            EventType.DEBUG_API,
            debugApi,
            Map.ofEntries(
                entry(Event.EventProperties.USER, auditInfo.actor().userId()),
                entry(Event.EventProperties.API_DEBUG_STATUS, ApiDebugStatus.TO_DEBUG.name()),
                entry(Event.EventProperties.GATEWAY_ID, selectedInstance.getId()),
                entry(Event.EventProperties.API_ID, debugApi.getId())
            )
        );
    }

    private static void validateDefinitionVersion(String apiId, DefinitionVersion debugApiDefinitionVersion) {
        if (!debugApiDefinitionVersion.equals(DefinitionVersion.V2)) {
            throw new DebugApiInvalidDefinitionVersionException(apiId);
        }
    }

    private static void validatePlan(DebugApi debugApi) {
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

    private Instance selectTargetGateway(String organizationId, String environmentId, Api apiDefinition) {
        return instanceQueryService
            .findAllStarted(organizationId, environmentId)
            .stream()
            .filter(Instance::isClusterPrimaryNode)
            .filter(instance -> instance.isRunningForEnvironment(environmentId))
            .filter(Instance::hasDebugPluginInstalled)
            .filter(instance -> EnvironmentUtils.hasMatchingTags(ofNullable(instance.getTags()), apiDefinition.getTags()))
            .max(Comparator.comparing(Instance::getStartedAt))
            .orElseThrow(() -> new DebugApiNoCompatibleInstanceException(apiDefinition.getId()));
    }

    private static void disableLoggingForDebug(DebugApi debugApi) {
        if (debugApi.getProxy() != null && debugApi.getProxy().getLogging() != null) {
            debugApi.getProxy().getLogging().setMode(LoggingMode.NONE);
            debugApi.getProxy().getLogging().setContent(LoggingContent.NONE);
            debugApi.getProxy().getLogging().setScope(LoggingScope.NONE);
        }
    }

    private static void disableHealthCheckForDebug(DebugApi debugApi) {
        if (debugApi.getServices() != null && debugApi.getServices().getHealthCheckService() != null) {
            debugApi.getServices().getHealthCheckService().setEnabled(false);
        }
    }

    @Builder
    public record Input(String apiId, DebugApi debugApi, AuditInfo auditInfo) {
        public Input {
            requireNonNull(apiId);
            requireNonNull(debugApi);
            requireNonNull(auditInfo);
        }
    }

    public record Output(Event debugApiEvent) {}
}
