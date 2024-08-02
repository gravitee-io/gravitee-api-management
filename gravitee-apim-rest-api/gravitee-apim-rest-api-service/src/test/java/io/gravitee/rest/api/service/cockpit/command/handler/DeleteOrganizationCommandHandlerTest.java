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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.organization.DeleteOrganizationCommand;
import io.gravitee.cockpit.api.command.v1.organization.DeleteOrganizationCommandPayload;
import io.gravitee.cockpit.api.command.v1.organization.DeleteOrganizationReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.api.TokenRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.repository.management.model.License;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.TagReferenceType;
import io.gravitee.repository.management.model.TenantReferenceType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.TokenReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteOrganizationCommandHandlerTest {

    private static final String EMPTY_ORG_ID = "org#1";
    private static final String ERROR_ENV_ID = "error";
    private static final String NOT_FOUND_ENV_ID = "not-found";
    private static final String ORG_ID = "org#2";
    private static final String USER_ID = "user#1";
    private static final String USER_ID_2 = "user#2";

    @Mock
    private AccessPointRepository accessPointRepository;

    @Mock
    private FlowRepository flowRepository;

    @Mock
    private ParameterRepository parameterRepository;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private CustomUserFieldsRepository customUserFieldsRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private IdentityProviderRepository identityProviderRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private PortalNotificationRepository portalNotificationRepository;

    @Mock
    private IdentityProviderActivationRepository identityProviderActivationRepository;

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventService eventService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private AccessPointCrudService accessPointService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    @Mock
    private SearchEngineService searchEngineService;

    private DeleteOrganizationCommandHandler cut;

    @Before
    public void setUp() throws Exception {
        when(organizationService.findByCockpitId(NOT_FOUND_ENV_ID)).thenThrow(new OrganizationNotFoundException(NOT_FOUND_ENV_ID));
        when(organizationService.findByCockpitId(ERROR_ENV_ID)).thenThrow(new RuntimeException(ERROR_ENV_ID));
        OrganizationEntity emptyOrganization = new OrganizationEntity();
        emptyOrganization.setId(EMPTY_ORG_ID);
        when(organizationService.findByCockpitId(EMPTY_ORG_ID)).thenReturn(emptyOrganization);
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(ORG_ID);
        when(organizationService.findByCockpitId(ORG_ID)).thenReturn(organization);
        when(userRepository.deleteByOrganizationId(ORG_ID)).thenReturn(List.of(USER_ID, USER_ID_2));

        cut =
            new DeleteOrganizationCommandHandler(
                accessPointRepository,
                auditRepository,
                commandRepository,
                customUserFieldsRepository,
                flowRepository,
                identityProviderActivationRepository,
                identityProviderRepository,
                licenseRepository,
                mediaRepository,
                membershipRepository,
                metadataRepository,
                parameterRepository,
                portalNotificationRepository,
                roleRepository,
                tagRepository,
                tenantRepository,
                tokenRepository,
                userRepository,
                accessPointService,
                environmentService,
                eventService,
                identityProviderActivationService,
                organizationService,
                searchEngineService
            );
    }

    @Test
    public void should_delete_empty_organization() {
        DeleteOrganizationReply reply = cut
            .handle(new DeleteOrganizationCommand(new DeleteOrganizationCommandPayload("delete-empty-org", EMPTY_ORG_ID, USER_ID)))
            .blockingGet();

        assertEquals(CommandStatus.SUCCEEDED, reply.getCommandStatus());
        verify(organizationService).delete(EMPTY_ORG_ID);
    }

    @Test
    public void should_not_delete_organization_with_environment() {
        when(environmentService.findByOrganization(EMPTY_ORG_ID)).thenReturn(List.of(EnvironmentEntity.builder().id("env-id").build()));

        DeleteOrganizationReply reply = cut
            .handle(new DeleteOrganizationCommand(new DeleteOrganizationCommandPayload("delete-empty-org", EMPTY_ORG_ID, USER_ID)))
            .blockingGet();

        assertEquals(CommandStatus.ERROR, reply.getCommandStatus());
        verify(organizationService, never()).delete(EMPTY_ORG_ID);
    }

    @Test
    public void should_delete_organization() throws TechnicalException {
        DeleteOrganizationReply reply = cut
            .handle(new DeleteOrganizationCommand(new DeleteOrganizationCommandPayload("delete-org", ORG_ID, USER_ID)))
            .blockingGet();

        assertEquals(CommandStatus.SUCCEEDED, reply.getCommandStatus());
        var context = new ExecutionContext(ORG_ID);
        verifyDisableOrganization(context);
        verifyDeleteOrganization(context);
    }

    @Test
    public void should_reply_succeeded_if_organization_not_found() {
        DeleteOrganizationReply reply = cut
            .handle(new DeleteOrganizationCommand(new DeleteOrganizationCommandPayload("delete-org", NOT_FOUND_ENV_ID, USER_ID)))
            .blockingGet();

        assertEquals(CommandStatus.SUCCEEDED, reply.getCommandStatus());
    }

    @Test
    public void should_reply_error_if_throw_an_error() {
        DeleteOrganizationReply reply = cut
            .handle(new DeleteOrganizationCommand(new DeleteOrganizationCommandPayload("delete-org", ERROR_ENV_ID, USER_ID)))
            .blockingGet();

        assertEquals(CommandStatus.ERROR, reply.getCommandStatus());
    }

    private void verifyDeleteOrganization(ExecutionContext executionContext) throws TechnicalException {
        verify(accessPointRepository)
            .deleteByReferenceIdAndReferenceType(executionContext.getOrganizationId(), AccessPointReferenceType.ORGANIZATION);
        verify(flowRepository).deleteByReferenceIdAndReferenceType(executionContext.getOrganizationId(), FlowReferenceType.ORGANIZATION);
        verify(parameterRepository)
            .deleteByReferenceIdAndReferenceType(executionContext.getOrganizationId(), ParameterReferenceType.ORGANIZATION);
        verify(auditRepository)
            .deleteByReferenceIdAndReferenceType(executionContext.getOrganizationId(), Audit.AuditReferenceType.ORGANIZATION);
        verify(customUserFieldsRepository)
            .deleteByReferenceIdAndReferenceType(executionContext.getOrganizationId(), CustomUserFieldReferenceType.ORGANIZATION);
        verify(userRepository).deleteByOrganizationId(executionContext.getOrganizationId());
        verify(searchEngineService).delete(executionContext, UserEntity.builder().id(USER_ID).build());
        verify(searchEngineService).delete(executionContext, UserEntity.builder().id(USER_ID_2).build());
        verify(metadataRepository).deleteByReferenceIdAndReferenceType(USER_ID, MetadataReferenceType.USER);
        verify(metadataRepository).deleteByReferenceIdAndReferenceType(USER_ID_2, MetadataReferenceType.USER);
        verify(portalNotificationRepository).deleteAll(USER_ID);
        verify(portalNotificationRepository).deleteAll(USER_ID_2);
        verify(tokenRepository).deleteByReferenceIdAndReferenceType(USER_ID, TokenReferenceType.USER.name());
        verify(tokenRepository).deleteByReferenceIdAndReferenceType(USER_ID_2, TokenReferenceType.USER.name());
        verify(tenantRepository)
            .deleteByReferenceIdAndReferenceType(executionContext.getOrganizationId(), TenantReferenceType.ORGANIZATION);
        verify(roleRepository).deleteByReferenceIdAndReferenceType(executionContext.getOrganizationId(), RoleReferenceType.ORGANIZATION);
        verify(membershipRepository)
            .deleteByReferenceIdAndReferenceType(executionContext.getOrganizationId(), MembershipReferenceType.ORGANIZATION);
        verify(identityProviderRepository).deleteByOrganizationId(executionContext.getOrganizationId());
        verify(identityProviderActivationRepository)
            .deleteByReferenceIdAndReferenceType(
                executionContext.getOrganizationId(),
                io.gravitee.repository.management.model.IdentityProviderActivationReferenceType.ORGANIZATION
            );
        verify(commandRepository).deleteByOrganizationId(executionContext.getOrganizationId());
        verify(licenseRepository).delete(ORG_ID, License.ReferenceType.ORGANIZATION);
        verify(tagRepository).deleteByReferenceIdAndReferenceType(ORG_ID, TagReferenceType.ORGANIZATION);
        verify(licenseRepository).delete(ORG_ID, License.ReferenceType.ORGANIZATION);
        verify(mediaRepository).deleteByOrganization(ORG_ID);
        verify(eventService).deleteOrUpdateEventsByOrganization(ORG_ID);
        verify(organizationService).delete(executionContext.getOrganizationId());
    }

    private void verifyDisableOrganization(ExecutionContext context) {
        verify(accessPointService).deleteAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, context.getOrganizationId());
        verify(identityProviderActivationService)
            .removeAllIdpsFromTarget(
                context,
                new IdentityProviderActivationService.ActivationTarget(
                    context.getOrganizationId(),
                    IdentityProviderActivationReferenceType.ORGANIZATION
                )
            );
    }

    @Test
    public void should_support_type() {
        assertEquals(CockpitCommandType.DELETE_ORGANIZATION.name(), cut.supportType());
    }
}
