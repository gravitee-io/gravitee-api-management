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
package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.environment.DeleteEnvironmentReply;
import io.gravitee.cockpit.api.command.v1.organization.DeleteOrganizationCommand;
import io.gravitee.cockpit.api.command.v1.organization.DeleteOrganizationReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.TokenReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeleteOrganizationCommandHandler implements CommandHandler<DeleteOrganizationCommand, DeleteOrganizationReply> {

    private final AccessPointCrudService accessPointService;
    private final AccessPointRepository accessPointRepository;
    private final AuditRepository auditRepository;
    private final CommandRepository commandRepository;
    private final CustomUserFieldsRepository customUserFieldsRepository;
    private final EnvironmentService environmentService;
    private final EventService eventService;
    private final FlowRepository flowRepository;
    private final IdentityProviderActivationRepository identityProviderActivationRepository;
    private final IdentityProviderActivationService identityProviderActivationService;
    private final IdentityProviderRepository identityProviderRepository;
    private final LicenseRepository licenseRepository;
    private final MediaRepository mediaRepository;
    private final MembershipRepository membershipRepository;
    private final MetadataRepository metadataRepository;
    private final OrganizationService organizationService;
    private final ParameterRepository parameterRepository;
    private final PortalNotificationRepository portalNotificationRepository;
    private final RoleRepository roleRepository;
    private final SearchEngineService searchEngineService;
    private final TagRepository tagRepository;
    private final TenantRepository tenantRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    public DeleteOrganizationCommandHandler(
        @Lazy AccessPointRepository accessPointRepository,
        @Lazy AuditRepository auditRepository,
        @Lazy CommandRepository commandRepository,
        @Lazy CustomUserFieldsRepository customUserFieldsRepository,
        @Lazy FlowRepository flowRepository,
        @Lazy IdentityProviderActivationRepository identityProviderActivationRepository,
        @Lazy IdentityProviderRepository identityProviderRepository,
        @Lazy LicenseRepository licenseRepository,
        @Lazy MediaRepository mediaRepository,
        @Lazy MembershipRepository membershipRepository,
        @Lazy MetadataRepository metadataRepository,
        @Lazy ParameterRepository parameterRepository,
        @Lazy PortalNotificationRepository portalNotificationRepository,
        @Lazy RoleRepository roleRepository,
        @Lazy TagRepository tagRepository,
        @Lazy TenantRepository tenantRepository,
        @Lazy TokenRepository tokenRepository,
        @Lazy UserRepository userRepository,
        AccessPointCrudService accessPointService,
        EnvironmentService environmentService,
        EventService eventService,
        IdentityProviderActivationService identityProviderActivationService,
        OrganizationService organizationService,
        SearchEngineService searchEngineService
    ) {
        this.accessPointRepository = accessPointRepository;
        this.accessPointService = accessPointService;
        this.auditRepository = auditRepository;
        this.commandRepository = commandRepository;
        this.customUserFieldsRepository = customUserFieldsRepository;
        this.environmentService = environmentService;
        this.eventService = eventService;
        this.flowRepository = flowRepository;
        this.identityProviderActivationRepository = identityProviderActivationRepository;
        this.identityProviderActivationService = identityProviderActivationService;
        this.identityProviderRepository = identityProviderRepository;
        this.licenseRepository = licenseRepository;
        this.mediaRepository = mediaRepository;
        this.membershipRepository = membershipRepository;
        this.metadataRepository = metadataRepository;
        this.organizationService = organizationService;
        this.parameterRepository = parameterRepository;
        this.portalNotificationRepository = portalNotificationRepository;
        this.roleRepository = roleRepository;
        this.searchEngineService = searchEngineService;
        this.tagRepository = tagRepository;
        this.tenantRepository = tenantRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String supportType() {
        return CockpitCommandType.DELETE_ORGANIZATION.name();
    }

    @Override
    public Single<DeleteOrganizationReply> handle(DeleteOrganizationCommand command) {
        var payload = command.getPayload();

        try {
            log.info("Delete organization with id [{}]", payload.cockpitId());
            var organization = organizationService.findByCockpitId(payload.cockpitId());
            var executionContext = new ExecutionContext(organization);
            List<EnvironmentEntity> environments = environmentService.findByOrganization(organization.getId());

            if (!environments.isEmpty()) {
                String errorDetails =
                    "Error occurred when deleting organization with id [%s] have [%s] environment(s)".formatted(
                            payload.id(),
                            environments.size()
                        );
                log.error(errorDetails);
                return Single.just(new DeleteOrganizationReply(command.getId(), errorDetails));
            }
            disableOrganization(executionContext);
            deleteOrganization(executionContext, organization);

            log.info("Organization [{}] with id [{}] has been deleted.", organization.getName(), organization.getId());
            return Single.just(new DeleteOrganizationReply(command.getId()));
        } catch (OrganizationNotFoundException e) {
            log.warn("Organization with cockpitId [{}] has not been found.", payload.cockpitId());
            return Single.just(new DeleteOrganizationReply(command.getId()));
        } catch (Exception e) {
            String errorDetails = "Error occurred when deleting organization with id [%s].".formatted(payload.id());
            log.error(errorDetails, e);
            return Single.just(new DeleteOrganizationReply(command.getId(), errorDetails));
        }
    }

    private void disableOrganization(ExecutionContext executionContext) {
        // Delete related access points
        this.accessPointService.deleteAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, executionContext.getOrganizationId());

        var context = new ExecutionContext(executionContext.getOrganizationId());

        // Deactivate all identity providers
        this.identityProviderActivationService.removeAllIdpsFromTarget(
                context,
                new IdentityProviderActivationService.ActivationTarget(
                    executionContext.getOrganizationId(),
                    IdentityProviderActivationReferenceType.ORGANIZATION
                )
            );
    }

    private void deleteOrganization(ExecutionContext executionContext, OrganizationEntity organization) throws TechnicalException {
        accessPointRepository.deleteByReferenceIdAndReferenceType(organization.getId(), AccessPointReferenceType.ORGANIZATION);
        flowRepository.deleteByReferenceIdAndReferenceType(organization.getId(), FlowReferenceType.ORGANIZATION);
        parameterRepository.deleteByReferenceIdAndReferenceType(organization.getId(), ParameterReferenceType.ORGANIZATION);
        customUserFieldsRepository.deleteByReferenceIdAndReferenceType(organization.getId(), CustomUserFieldReferenceType.ORGANIZATION);
        membershipRepository.deleteByReferenceIdAndReferenceType(
            executionContext.getOrganizationId(),
            MembershipReferenceType.ORGANIZATION
        );
        eventService.deleteOrUpdateEventsByOrganization(executionContext.getOrganizationId());
        userRepository
            .deleteByOrganizationId(organization.getId())
            .forEach(userId -> {
                searchEngineService.delete(executionContext, UserEntity.builder().id(userId).build());
                try {
                    portalNotificationRepository.deleteAll(userId);
                    metadataRepository.deleteByReferenceIdAndReferenceType(userId, MetadataReferenceType.USER);
                    tokenRepository.deleteByReferenceIdAndReferenceType(userId, TokenReferenceType.USER.name());
                } catch (TechnicalException e) {
                    throw new TechnicalManagementException(e);
                }
            });
        tenantRepository.deleteByReferenceIdAndReferenceType(organization.getId(), TenantReferenceType.ORGANIZATION);
        roleRepository.deleteByReferenceIdAndReferenceType(organization.getId(), RoleReferenceType.ORGANIZATION);
        identityProviderRepository.deleteByOrganizationId(organization.getId());
        identityProviderActivationRepository.deleteByReferenceIdAndReferenceType(
            organization.getId(),
            io.gravitee.repository.management.model.IdentityProviderActivationReferenceType.ORGANIZATION
        );
        licenseRepository.delete(organization.getId(), License.ReferenceType.ORGANIZATION);
        tagRepository.deleteByReferenceIdAndReferenceType(organization.getId(), TagReferenceType.ORGANIZATION);
        commandRepository.deleteByOrganizationId(organization.getId());
        mediaRepository.deleteByOrganization(organization.getId());
        organizationService.delete(organization.getId());
        auditRepository.deleteByReferenceIdAndReferenceType(organization.getId(), Audit.AuditReferenceType.ORGANIZATION);
    }
}
