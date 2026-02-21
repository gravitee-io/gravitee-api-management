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
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.LoggingContent;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.LoggingScope;
import io.gravitee.definition.model.debug.DebugApiProxy;
import io.gravitee.definition.model.debug.DebugApiV2;
import io.gravitee.definition.model.debug.DebugApiV4;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
@UseCase
public class DebugApiUseCase {

    private final ApiPolicyValidatorDomainService apiPolicyValidatorDomainService;
    private final ApiCrudService apiCrudService;
    private final InstanceQueryService instanceQueryService;
    private final PlanQueryService planQueryService;
    private final FlowCrudService flowCrudService;
    private final EventCrudService eventCrudService;

    public DebugApiUseCase.Output execute(DebugApiUseCase.Input input) throws InvalidPathsException {
        log.debug("Debugging API {}", input.apiId);

        if (input.debugApiV2 != null) {
            return new DebugApiUseCase.Output(handleEmbeddedDebugApiDefinition(input.debugApiV2, input.auditInfo));
        }

        if (input.debugApiRequest != null) {
            return new DebugApiUseCase.Output(handleDebugApi(input.apiId, input.debugApiRequest, input.auditInfo));
        }

        throw new IllegalArgumentException("Invalid input: either debugApiV2 or debugApiRequest must be provided");
    }

    private Event handleEmbeddedDebugApiDefinition(DebugApiV2 debugApiV2, AuditInfo auditInfo) {
        if (debugApiV2.getDefinitionVersion() != DefinitionVersion.V2) {
            throw new DebugApiInvalidDefinitionVersionException(debugApiV2.getId());
        }

        if (!apiCrudService.existsById(debugApiV2.getId())) {
            throw new ApiNotFoundException(debugApiV2.getId());
        }

        // Check policy configuration
        apiPolicyValidatorDomainService.checkPolicyConfigurations(debugApiV2);

        final Instance selectedGateway = selectTargetGateway(auditInfo.organizationId(), auditInfo.environmentId(), debugApiV2);

        // prepare and validate debugApiV2
        validatePlans(debugApiV2.getId(), debugApiV2.getPlans(), null);
        disableLoggingForDebug(debugApiV2);
        disableHealthCheckForDebug(debugApiV2);

        return createDebugApiEvent(auditInfo, debugApiV2, selectedGateway);
    }

    private Event handleDebugApi(String apiId, HttpRequest debugRequest, AuditInfo auditInfo) {
        var debugApi = validateApiExists(apiId, debugRequest);

        // Check policy configuration
        apiPolicyValidatorDomainService.checkPolicyConfigurations(debugApi);

        final Instance selectedGateway = selectTargetGateway(auditInfo.organizationId(), auditInfo.environmentId(), debugApi);

        // prepare and validate debugApi
        disableLoggingForDebug(debugApi);
        disableHealthCheckForDebug(debugApi);

        return createDebugApiEvent(auditInfo, debugApi, selectedGateway);
    }

    private DebugApiProxy validateApiExists(String apiId, HttpRequest debugApiRequest) {
        var api = apiCrudService.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

        return switch (api.getApiDefinitionValue()) {
            case io.gravitee.definition.model.v4.Api v4Api -> {
                if (api.getType() != ApiType.PROXY) {
                    throw new DebugApiInvalidDefinitionVersionException(apiId);
                }
                var plans = planQueryService
                    .findAllByReferenceIdAndReferenceType(apiId, GenericPlanEntity.ReferenceType.API.name())
                    .stream()
                    .map(io.gravitee.apim.core.plan.model.Plan::getPlanDefinitionHttpV4)
                    .map(plan -> plan.flows(flowCrudService.getPlanV4Flows(plan.getId())))
                    .toList();

                validatePlans(apiId, null, plans);
                yield new DebugApiV4(v4Api.plans(plans).flow(flowCrudService.getApiV4Flows(apiId)), debugApiRequest);
            }
            case io.gravitee.definition.model.Api v2Api -> {
                var plans = planQueryService
                    .findAllByReferenceIdAndReferenceType(apiId, GenericPlanEntity.ReferenceType.API.name())
                    .stream()
                    .map(io.gravitee.apim.core.plan.model.Plan::getPlanDefinitionV2)
                    .map(plan -> plan.flows(flowCrudService.getPlanV2Flows(plan.getId())))
                    .toList();
                validatePlans(apiId, plans, null);
                yield new DebugApiV2(v2Api.plans(plans).flows(flowCrudService.getApiV2Flows(apiId)), debugApiRequest);
            }
            default -> throw new DebugApiInvalidDefinitionVersionException(apiId);
        };
    }

    private Event createDebugApiEvent(AuditInfo auditInfo, DebugApiProxy debugApi, Instance selectedInstance) {
        var eventProperties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        eventProperties.put(Event.EventProperties.USER, auditInfo.actor().userId());
        eventProperties.put(Event.EventProperties.API_DEBUG_STATUS, ApiDebugStatus.TO_DEBUG.name());
        eventProperties.put(Event.EventProperties.GATEWAY_ID, selectedInstance.getId());
        eventProperties.put(Event.EventProperties.API_ID, debugApi.getId());

        if (debugApi.getDefinitionVersion() == DefinitionVersion.V4) {
            eventProperties.put(Event.EventProperties.API_DEFINITION_VERSION, DefinitionVersion.V4.name());
        }

        return eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            EventType.DEBUG_API,
            debugApi,
            eventProperties
        );
    }

    private static void validatePlans(String apiId, List<io.gravitee.definition.model.Plan> plansV2, List<Plan> plansV4) {
        if (plansV2 != null) {
            boolean hasValidPlan = plansV2
                .stream()
                .anyMatch(
                    plan ->
                        PlanStatus.STAGING.name().equalsIgnoreCase(plan.getStatus()) ||
                        PlanStatus.PUBLISHED.name().equalsIgnoreCase(plan.getStatus())
                );

            if (!hasValidPlan) {
                throw new DebugApiNoValidPlanException(apiId);
            }
        }

        if (plansV4 != null) {
            boolean hasValidPlan = plansV4
                .stream()
                .map(Plan::getStatus)
                .anyMatch(
                    status ->
                        status == io.gravitee.definition.model.v4.plan.PlanStatus.STAGING ||
                        status == io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED
                );

            if (!hasValidPlan) {
                throw new DebugApiNoValidPlanException(apiId);
            }
        }
    }

    private Instance selectTargetGateway(String organizationId, String environmentId, DebugApiProxy debugApi) {
        return instanceQueryService
            .findAllStarted(organizationId, environmentId)
            .stream()
            .filter(Instance::isClusterPrimaryNode)
            .filter(instance -> instance.isRunningForEnvironment(environmentId))
            .filter(Instance::hasDebugPluginInstalled)
            .filter(instance -> EnvironmentUtils.hasMatchingTags(ofNullable(instance.getTags()), debugApi.getTags()))
            .max(Comparator.comparing(Instance::getStartedAt))
            .orElseThrow(() -> new DebugApiNoCompatibleInstanceException(debugApi.getId()));
    }

    private static void disableLoggingForDebug(DebugApiProxy debugApi) {
        if (debugApi instanceof DebugApiV2 debugApiV2) {
            if (debugApiV2.getProxy() != null && debugApiV2.getProxy().getLogging() != null) {
                debugApiV2.getProxy().getLogging().setMode(LoggingMode.NONE);
                debugApiV2.getProxy().getLogging().setContent(LoggingContent.NONE);
                debugApiV2.getProxy().getLogging().setScope(LoggingScope.NONE);
            }
        }

        if (debugApi instanceof DebugApiV4 debugApiV4) {
            var apiDefinition = debugApiV4.getApiDefinition();
            if (apiDefinition.getAnalytics() != null) {
                apiDefinition.getAnalytics().setLogging(null);
            }
        }
    }

    private static void disableHealthCheckForDebug(DebugApiProxy debugApi) {
        if (debugApi instanceof DebugApiV2 debugApiV2) {
            if (debugApiV2.getServices() != null && debugApiV2.getServices().getHealthCheckService() != null) {
                debugApiV2.getServices().getHealthCheckService().setEnabled(false);
            }
        }
        if (debugApi instanceof DebugApiV4 debugApiV4) {
            var apiDefinition = debugApiV4.getApiDefinition();
            apiDefinition
                .getEndpointGroups()
                .stream()
                .map(EndpointGroup::getServices)
                .forEach(endpointGroupServices -> endpointGroupServices.setHealthCheck(null));
            apiDefinition
                .getEndpointGroups()
                .stream()
                .flatMap(group -> group.getEndpoints().stream())
                .map(Endpoint::getServices)
                .forEach(endpointServices -> endpointServices.setHealthCheck(null));
        }
    }

    /**
     * Input for the DebugApiUseCase.
     *
     * @param apiId           The API id
     * @param debugApiV2      In case of debugging an API V2, the REST endpoint is called with the entire API definition because the debug UI is included in the Policy Studio.
     * @param debugApiRequest In case of debugging an API V4, the REST endpoint is called without API Definition because the debug UI has been moved outside the Policy Studio.
     * @param auditInfo
     */
    public record Input(String apiId, DebugApiV2 debugApiV2, HttpRequest debugApiRequest, AuditInfo auditInfo) {
        public Input {
            requireNonNull(apiId);
            requireNonNull(auditInfo);
        }

        public Input(String apiId, HttpRequest debugApiRequest, AuditInfo auditInfo) {
            this(apiId, null, debugApiRequest, auditInfo);
            requireNonNull(debugApiRequest);
        }

        public Input(String apiId, DebugApiV2 debugApiV2, AuditInfo auditInfo) {
            this(apiId, debugApiV2, null, auditInfo);
            requireNonNull(debugApiV2);
        }
    }

    public record Output(Event debugApiEvent) {}
}
