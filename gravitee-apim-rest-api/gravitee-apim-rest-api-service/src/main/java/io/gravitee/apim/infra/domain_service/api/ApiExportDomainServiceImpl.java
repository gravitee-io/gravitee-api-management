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
package io.gravitee.apim.infra.domain_service.api;

import static io.gravitee.apim.core.audit.model.Excludable.GROUPS;
import static io.gravitee.apim.core.audit.model.Excludable.MEMBERS;
import static io.gravitee.apim.core.audit.model.Excludable.METADATA;
import static io.gravitee.apim.core.audit.model.Excludable.PAGES_MEDIA;
import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_DOCUMENTATION;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_MEMBER;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static java.util.function.Predicate.not;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PageExport;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.Excludable;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.media.query_service.MediaQueryService;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.apim.infra.adapter.GraviteeDefinitionAdapter;
import io.gravitee.apim.infra.adapter.MemberAdapter;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiExportDomainServiceImpl implements ApiExportDomainService {

    private static final GraviteeDefinitionAdapter DEFINITION_ADAPTER = GraviteeDefinitionAdapter.INSTANCE;
    private final PermissionService permissionService;
    private final MediaQueryService mediaService;

    private final WorkflowCrudService workflowCrudService;
    private final MembershipCrudService membershipCrudService;
    private final UserCrudService userCrudService;
    private final RoleQueryService roleQueryService;
    private final MetadataCrudService metadataCrudService;
    private final PageQueryService pageQueryService;
    private final ApiCrudService apiCrudService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final PlanCrudService planCrudService;
    private final IntegrationCrudService integrationCrudService;
    private final FlowCrudService flowCrudService;

    @Override
    public GraviteeDefinition export(String apiId, AuditInfo auditInfo, Collection<Excludable> excluded) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var excludeIds = excluded.contains(Excludable.IDS);

        var apiPrimaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(auditInfo.organizationId(), apiId);
        var api1 = apiCrudService.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

        var members = !excluded.contains(MEMBERS) ? exportApiMembers(apiId, auditInfo, executionContext) : null;
        var metadata = !excluded.contains(METADATA) ? exportApiMetadata(apiId, auditInfo, executionContext) : null;

        var pages = !excluded.contains(PAGES_MEDIA) ? exportApiPages(apiId, auditInfo, executionContext) : null;
        var medias = !excluded.contains(PAGES_MEDIA) ? exportApiMedia(apiId, auditInfo, executionContext) : null;

        var workflowState = stream(workflowCrudService.findByApiId(apiId))
            .map(Workflow::getState)
            .map(Enum::name)
            .map(WorkflowState::valueOf)
            .findFirst()
            .orElse(null);

        var groups = !excluded.contains(GROUPS) ? api1.getGroups() : null;
        return switch (api1.getApiDefinitionValue()) {
            case Api v2 -> {
                Function<Plan, PlanDescriptor.V2> mapPlanV2 = plan -> {
                    var mapped = DEFINITION_ADAPTER.mapPlanV2(plan, excludeIds);
                    return planWithFlowV2(mapped, plan.getId(), excludeIds);
                };
                var plans = mapPlan(apiId, mapPlanV2, excluded);
                var flows = getApiV2Flows(apiId, excludeIds);
                var api = DEFINITION_ADAPTER.mapV2(api1, apiPrimaryOwner, workflowState, groups, metadata, flows, excludeIds);
                yield GraviteeDefinition.from(api, members, metadata, pages, plans, medias, api1.getPicture(), api1.getBackground());
            }
            case io.gravitee.definition.model.v4.Api apiV4 -> {
                Function<Plan, PlanDescriptor.V4> mapPlanV4 = plan -> {
                    var mapped = DEFINITION_ADAPTER.mapPlanV4(plan, excludeIds);
                    return planWithFlowV4(mapped, plan.getId(), excludeIds);
                };
                var plans = mapPlan(apiId, mapPlanV4, excluded);
                var flows = getApiV4Flows(apiId, excludeIds);
                var api = DEFINITION_ADAPTER.mapV4(api1, apiV4, apiPrimaryOwner, workflowState, groups, metadata, flows, excludeIds);
                yield GraviteeDefinition.from(api, members, metadata, pages, plans, medias, api1.getPicture(), api1.getBackground());
            }
            case NativeApi nativeApi -> {
                Function<Plan, PlanDescriptor.Native> mapPlanNative = plan -> {
                    var mapped = DEFINITION_ADAPTER.mapPlanNative(plan, excludeIds);
                    return planWithFlowNative(mapped, plan.getId(), excludeIds);
                };
                var plans = mapPlan(apiId, mapPlanNative, excluded);
                var flows = getNativeApiFlows(apiId, excludeIds);
                var api = DEFINITION_ADAPTER.mapNative(
                    api1,
                    nativeApi,
                    apiPrimaryOwner,
                    workflowState,
                    groups,
                    metadata,
                    flows,
                    excludeIds
                );
                yield GraviteeDefinition.from(api, members, metadata, pages, plans, medias, api1.getPicture(), api1.getBackground());
            }
            case FederatedApi federatedApi -> {
                Function<Plan, PlanDescriptor.Federated> mapPlanFederated = DEFINITION_ADAPTER::mapPlanFederated;
                var plans = mapPlan(apiId, mapPlanFederated, excluded);
                var integ = api1.getOriginContext() instanceof OriginContext.Integration ori && ori.integrationName() == null
                    ? integrationCrudService.findApiIntegrationById(ori.integrationId()).orElse(null)
                    : null;
                var api = DEFINITION_ADAPTER.mapFederated(api1, federatedApi, apiPrimaryOwner, workflowState, groups, metadata, integ);
                yield GraviteeDefinition.from(api, members, metadata, pages, plans, medias, api1.getPicture(), api1.getBackground());
            }
            case FederatedAgent agent -> null; // TODO
            default -> throw new ApiDefinitionVersionNotSupportedException(api1.getVersion());
        };
    }

    private Set<ApiMember> exportApiMembers(String apiId, AuditInfo auditInfo, ExecutionContext executionContext) {
        if (!permissionService.hasPermission(executionContext, auditInfo.actor().userId(), API_MEMBER, apiId, READ)) {
            return null;
        }
        List<Membership> members = stream(membershipCrudService.findByApiId(apiId))
            .filter(memberEntity -> memberEntity.getMemberType() == Membership.Type.USER)
            .toList();
        var userIds = members.stream().map(Membership::getMemberId).distinct().toList();
        var userByIds = stream(userCrudService.findBaseUsersByIds(userIds)).collect(
            Collectors.toMap(BaseUserEntity::getId, Function.identity())
        );
        var roles = stream(roleQueryService.findByIds(members.stream().map(Membership::getRoleId).collect(Collectors.toSet()))).collect(
            Collectors.toMap(Role::getId, Function.identity())
        );

        return members
            .stream()
            .map(m -> MemberAdapter.INSTANCE.toApiMember(m, userByIds.get(m.getMemberId()), roles.get(m.getRoleId())))
            .collect(Collectors.toSet());
    }

    private Collection<NewApiMetadata> exportApiMetadata(String apiId, AuditInfo auditInfo, ExecutionContext executionContext) {
        if (!permissionService.hasPermission(executionContext, auditInfo.actor().userId(), RolePermission.API_METADATA, apiId, READ)) {
            return null;
        }
        return Stream.concat(
            stream(metadataCrudService.findByEnvId(executionContext.getEnvironmentId())),
            stream(metadataCrudService.findByApiId(apiId))
        )
            .map(DEFINITION_ADAPTER::mapMetadata)
            .collect(
                Collectors.toMap(NewApiMetadata::getKey, Function.identity(), (envMetadata, apiMetadata) -> {
                    apiMetadata.setDefaultValue(envMetadata.getValue());
                    return apiMetadata;
                })
            )
            .values();
    }

    private List<PageExport> exportApiPages(String apiId, AuditInfo auditInfo, ExecutionContext executionContext) {
        return permissionService.hasPermission(executionContext, auditInfo.actor().userId(), API_DOCUMENTATION, apiId, READ)
            ? DEFINITION_ADAPTER.mapPage(pageQueryService.searchByApiId(apiId))
            : null;
    }

    private List<Media> exportApiMedia(String apiId, AuditInfo auditInfo, ExecutionContext executionContext) {
        return permissionService.hasPermission(executionContext, auditInfo.actor().userId(), API_DOCUMENTATION, apiId, READ)
            ? mediaService.findAllByApiId(apiId)
            : null;
    }

    @Nullable
    private <T> Collection<T> mapPlan(String apiId, Function<Plan, T> mapper, Collection<Excludable> excluded) {
        return excluded.contains(Excludable.PLANS)
            ? null
            : stream(planCrudService.findByApiId(apiId)).filter(not(Plan::isClosed)).map(mapper).toList();
    }

    private PlanDescriptor.V4 planWithFlowV4(PlanDescriptor.V4 planV4, String planId, boolean excludeIds) {
        var flows = flowCrudService.getPlanV4Flows(planId);
        if (excludeIds) {
            flows.forEach(f -> f.setId(null));
        }
        return planV4.withFlow(flows);
    }

    private PlanDescriptor.V2 planWithFlowV2(PlanDescriptor.V2 planV2, String planId, boolean excludeIds) {
        var flows = flowCrudService.getPlanV2Flows(planId);
        if (excludeIds) {
            flows.forEach(f -> f.setId(null));
        }
        return planV2.withFlow(flows);
    }

    private PlanDescriptor.Native planWithFlowNative(PlanDescriptor.Native planNative, String planId, boolean excludeIds) {
        var flows = flowCrudService.getNativePlanFlows(planId);
        if (excludeIds) {
            flows.forEach(f -> f.setId(null));
        }
        return planNative.withFlow(flows);
    }

    private List<io.gravitee.definition.model.flow.Flow> getApiV2Flows(String apiId, boolean excludeIds) {
        var flows = flowCrudService.getApiV2Flows(apiId);
        if (excludeIds) {
            flows.forEach(f -> f.setId(null));
        }
        return flows;
    }

    private List<io.gravitee.definition.model.v4.flow.Flow> getApiV4Flows(String apiId, boolean excludeIds) {
        var flows = flowCrudService.getApiV4Flows(apiId);
        if (excludeIds) {
            flows.forEach(f -> f.setId(null));
        }
        return flows;
    }

    private List<NativeFlow> getNativeApiFlows(String apiId, boolean excludeIds) {
        var flows = flowCrudService.getNativeApiFlows(apiId);
        if (excludeIds) {
            flows.forEach(f -> f.setId(null));
        }
        return flows;
    }
}
