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

import static io.gravitee.repository.management.model.Api.AuditEvent.API_CREATED;
import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.api.domain_service.CreateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.user.domain_service.UserPrimaryOwnerDomainService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.DefinitionVersionException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class CreateFederatedApiDomainServiceImpl implements CreateFederatedApiDomainService {

    private final ApiRepository apiRepository;

    private final GroupService groupService;

    private final MembershipService membershipService;

    private final UserPrimaryOwnerDomainService userPrimaryOwnerDomainService;

    private final SearchEngineService searchEngineService;

    private final AuditService auditService;

    public CreateFederatedApiDomainServiceImpl(
        @Lazy ApiRepository apiRepository,
        GroupService groupService,
        MembershipService membershipService,
        UserPrimaryOwnerDomainService userPrimaryOwnerDomainService,
        SearchEngineService searchEngineService,
        AuditService auditService
    ) {
        this.apiRepository = apiRepository;
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.userPrimaryOwnerDomainService = userPrimaryOwnerDomainService;
        this.searchEngineService = searchEngineService;
        this.auditService = auditService;
    }

    @Override
    public Api create(Api api, AuditInfo auditInfo) {
        ExecutionContext executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        String id = api.getId() != null && !api.getId().isEmpty() ? api.getId() : UuidString.generateRandom();
        api.setId(id);
        api.getApiDefinitionFederated().setId(id);

        log.debug("Creating Federated API {}", id);
        try {
            apiRepository
                .findById(id)
                .ifPresent(action -> {
                    throw new ApiAlreadyExistsException(id);
                });
        } catch (TechnicalException ex) {
            String errorMsg = String.format("An error occurs while trying to check if 'id' %s is already used", id);
            log.error(errorMsg, ex);
            throw new TechnicalDomainException(errorMsg, ex);
        }

        PrimaryOwnerEntity primaryOwnerEntity = userPrimaryOwnerDomainService.getUserPrimaryOwner(auditInfo.actor().userId());

        this.validateAndSanitizeImportFederatedApiForCreation(api);

        api.setEnvironmentId(executionContext.getEnvironmentId());
        io.gravitee.repository.management.model.Api repositoryApi = ApiAdapter.INSTANCE.toRepository(api);
        // Set date fields
        repositoryApi.setCreatedAt(new Date());
        repositoryApi.setUpdatedAt(repositoryApi.getCreatedAt());

        repositoryApi.setApiLifecycleState(ApiLifecycleState.CREATED);
        if (api.getDefinitionContext().isOriginManagement()) {
            repositoryApi.setLifecycleState(LifecycleState.STOPPED);
        }

        // Make sure visibility is PRIVATE by default if not set.
        repositoryApi.setVisibility(api.getVisibility() == null ? Visibility.PRIVATE : Visibility.valueOf(api.getVisibility().toString()));

        // Add Default groups
        Set<String> defaultGroups = groupService
            .findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)
            .stream()
            .map(GroupEntity::getId)
            .collect(toSet());
        if (repositoryApi.getGroups() == null) {
            repositoryApi.setGroups(defaultGroups.isEmpty() ? null : defaultGroups);
        } else {
            repositoryApi.getGroups().addAll(defaultGroups);
        }

        // if po is a group, add it as a member of the API
        if (ApiPrimaryOwnerMode.GROUP.name().equals(primaryOwnerEntity.type())) {
            if (repositoryApi.getGroups() == null) {
                repositoryApi.setGroups(new HashSet<>());
            }
            repositoryApi.getGroups().add(primaryOwnerEntity.id());
        }

        io.gravitee.repository.management.model.Api createdApi;
        try {
            createdApi = apiRepository.create(repositoryApi);
            log.debug("API {} imported", createdApi.getId());
        } catch (TechnicalException ex) {
            String errorMsg = String.format("An error occurs while trying to create '%s' for user '%s'", api, auditInfo.actor().userId());
            log.error(errorMsg, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }

        // TODO manage context information (access point, runtime ...)

        // Audit
        auditService.createApiAuditLog(
            executionContext,
            createdApi.getId(),
            Collections.emptyMap(),
            API_CREATED,
            createdApi.getCreatedAt(),
            null,
            createdApi
        );

        // Add the primary owner of the newly created API
        addPrimaryOwnerToCreatedApi(executionContext, primaryOwnerEntity, createdApi);

        searchEngineService.index(executionContext, ApiAdapter.INSTANCE.toFederatedApiEntity(createdApi, primaryOwnerEntity), false);

        return ApiAdapter.INSTANCE.toCoreModel(createdApi);
    }

    private void validateAndSanitizeImportFederatedApiForCreation(final Api api) {
        // Validate version
        this.validateDefinitionVersion(null, api.getDefinitionVersion());
        // Validate and clean lifecycle state. In creation, lifecycle state can't be set.
        api.setLifecycleState(null);
    }

    private void validateDefinitionVersion(final DefinitionVersion oldDefinitionVersion, final DefinitionVersion newDefinitionVersion) {
        if (newDefinitionVersion != DefinitionVersion.FEDERATED) {
            throw new InvalidDataException("Definition version is unsupported, should be FEDERATED");
        }
        if (oldDefinitionVersion != null && oldDefinitionVersion.asInteger() > newDefinitionVersion.asInteger()) {
            // not allowed downgrading definition version
            throw new DefinitionVersionException();
        }
    }

    private void addPrimaryOwnerToCreatedApi(
        ExecutionContext executionContext,
        PrimaryOwnerEntity primaryOwner,
        io.gravitee.repository.management.model.Api createdApi
    ) {
        membershipService.addRoleToMemberOnReference(
            executionContext,
            new MembershipService.MembershipReference(MembershipReferenceType.API, createdApi.getId()),
            new MembershipService.MembershipMember(primaryOwner.id(), null, MembershipMemberType.valueOf(primaryOwner.type())),
            new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
        );
    }
}
