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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@UseCase
public class UpdateApiGroupsUseCase {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;
    private final ValidateGroupsDomainService validateGroupsDomainService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final PermissionDomainService permissionDomainService;
    private final MembershipQueryService membershipQueryService;
    private final GroupQueryService groupQueryService;

    public record Input(String apiId, Set<String> groups, AuditInfo auditInfo) {}

    public record Output(Set<String> groups) {}

    public Output execute(Input input) {
        Api api = apiCrudService.get(input.apiId());

        if (!api.getEnvironmentId().equals(input.auditInfo().environmentId())) {
            throw new ApiNotFoundException(input.apiId());
        }

        if (api.getOriginContext() instanceof OriginContext.Kubernetes) {
            throw new ValidationDomainException("Cannot update groups of a Kubernetes-managed API");
        }

        Set<String> requestedGroups = groupsTheCallerMayAssign(input, api.getGroups());
        var validationInput = new ValidateGroupsDomainService.Input(
            input.auditInfo().environmentId(),
            requestedGroups,
            api.getDefinitionVersion().getLabel(),
            api.getOriginContext().name()
        );
        var validationResult = validateGroupsDomainService.validateAndSanitize(validationInput);
        Set<String> sanitizedGroups = new HashSet<>(
            validationResult.value().map(ValidateGroupsDomainService.Input::groups).orElse(input.groups())
        );
        primaryOwnerGroupId(input, api.getId()).ifPresent(sanitizedGroups::add);

        Set<String> oldGroups = api.getGroups();

        api.setGroups(sanitizedGroups);
        Api updated = apiCrudService.update(api);

        createAuditLog(oldGroups, updated.getGroups(), input.apiId(), input.auditInfo());

        return new Output(updated.getGroups());
    }

    private Set<String> groupsTheCallerMayAssign(Input input, Set<String> alreadyOnApi) {
        Set<String> requested = input.groups();
        if (requested == null || requested.isEmpty()) {
            return requested;
        }
        String userId = input.auditInfo().actor().userId();
        if (
            permissionDomainService.hasPermission(
                input.auditInfo().organizationId(),
                userId,
                RolePermission.ENVIRONMENT_GROUP,
                input.auditInfo().environmentId(),
                RolePermissionAction.READ
            )
        ) {
            return requested;
        }
        Set<String> allowed = new HashSet<>();
        if (alreadyOnApi != null) {
            allowed.addAll(alreadyOnApi);
        }
        if (userId != null && !userId.isBlank()) {
            Set<String> memberGroupIds = membershipQueryService
                .findByMemberIdAndMemberTypeAndReferenceType(userId, Membership.Type.USER, Membership.ReferenceType.GROUP)
                .stream()
                .map(Membership::getReferenceId)
                .collect(Collectors.toSet());
            if (!memberGroupIds.isEmpty()) {
                groupQueryService
                    .findByIds(memberGroupIds)
                    .stream()
                    .filter(group -> input.auditInfo().environmentId().equals(group.getEnvironmentId()))
                    .map(Group::getId)
                    .forEach(allowed::add);
            }
        }
        return requested.stream().filter(allowed::contains).collect(Collectors.toSet());
    }

    private Optional<String> primaryOwnerGroupId(Input input, String apiId) {
        try {
            PrimaryOwnerEntity primaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(input.auditInfo().organizationId(), apiId);
            if (primaryOwner != null && PrimaryOwnerEntity.Type.GROUP.equals(primaryOwner.type())) {
                return Optional.of(primaryOwner.id());
            }
        } catch (ApiPrimaryOwnerNotFoundException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private void createAuditLog(Set<String> groupsBeforeUpdate, Set<String> groupsAfterUpdate, String apiId, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .apiId(apiId)
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ApiAuditEvent.API_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(groupsBeforeUpdate)
                .newValue(groupsAfterUpdate)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.API, apiId))
                .build()
        );
    }
}
