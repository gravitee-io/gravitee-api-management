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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.environment.DeleteEnvironmentCommand;
import io.gravitee.cockpit.api.command.v1.environment.DeleteEnvironmentCommandPayload;
import io.gravitee.cockpit.api.command.v1.environment.DeleteEnvironmentReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.api.WorkflowRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.repository.management.model.DashboardReferenceType;
import io.gravitee.repository.management.model.InvitationReferenceType;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.repository.management.model.PortalNotificationDefaultReferenceId;
import io.gravitee.repository.management.model.RatingReferenceType;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.ThemeReferenceType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteEnvironmentCommandHandlerTest {

    private static final String EMPTY_ENV_ID = "env#1";

    private static final String ENV_ID = "env#2";
    private static final String USER_ID = "user#1";
    private static final String API_ID_1 = "api#1";
    private static final String API_ID_2 = "api#2";
    private static final String APP_ID_1 = "app#1";
    private static final String APP_ID_2 = "app#2";
    private static final String ORG_ID = "org#1";
    private static final String DICTIONARY_ID = "dic#1";
    private static final String PLAN_ID_1 = "plan#1";
    private static final String PLAN_ID_2 = "plan#2";
    private static final String ALERT_ID_1 = "alert#1";
    private static final String GROUP_ID_1 = "group#1";
    private static final String GROUP_ID_2 = "group#2";
    private static final String NOT_FOUND_ENV_ID = "not-found";
    private static final String ERROR_ENV_ID = "error";
    private static final String SUBSCRIPTION_ID_1 = "subscription#1";
    private static final String SUBSCRIPTION_ID_2 = "subscription#2";

    @Mock
    private AccessPointRepository accessPointRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiHeaderRepository apiHeaderRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FlowRepository flowRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PageRevisionRepository pageRevisionRepository;

    @Mock
    private ParameterRepository parameterRepository;

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Mock
    private ApiCategoryOrderRepository apiCategoryOrderRepository;

    @Mock
    private ApiQualityRuleRepository apiQualityRuleRepository;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private CustomUserFieldsRepository customUserFieldsRepository;

    @Mock
    private IntegrationRepository integrationRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private DictionaryRepository dictionaryRepository;

    @Mock
    private GenericNotificationConfigRepository genericNotificationConfigRepository;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private IdentityProviderActivationRepository identityProviderActivationRepository;

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingAnswerRepository ratingAnswerRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiStateService apiStateService;

    @Mock
    private EventService eventService;

    @Mock
    private AccessPointCrudService accessPointService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    @Mock
    private DictionaryService dictionaryService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApplicationAlertService applicationAlertService;

    @Mock
    private SearchEngineService searchEngineService;

    private DeleteEnvironmentCommandHandler cut;

    @Before
    public void setUp() throws Exception {
        when(environmentService.findByCockpitId(NOT_FOUND_ENV_ID)).thenThrow(new EnvironmentNotFoundException(NOT_FOUND_ENV_ID));
        when(environmentService.findByCockpitId(ERROR_ENV_ID)).thenThrow(new RuntimeException(ERROR_ENV_ID));
        EnvironmentEntity emptyEnvironment = new EnvironmentEntity();
        emptyEnvironment.setId(EMPTY_ENV_ID);
        emptyEnvironment.setName("Empty environment to delete");
        when(environmentService.findByCockpitId(EMPTY_ENV_ID)).thenReturn(emptyEnvironment);
        when(
            apiRepository.search(
                new ApiCriteria.Builder().state(LifecycleState.STARTED).environmentId(EMPTY_ENV_ID).build(),
                new ApiFieldFilter.Builder().excludeDefinition().excludePicture().build()
            )
        )
            .thenReturn(List.of());

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENV_ID);
        environment.setName("Environment to delete");
        environment.setOrganizationId(ORG_ID);
        when(environmentService.findByCockpitId(ENV_ID)).thenReturn(environment);
        when(dictionaryService.findAll(new ExecutionContext(ORG_ID, ENV_ID)))
            .thenReturn(Set.of(DictionaryEntity.builder().id(DICTIONARY_ID).build()));

        Api api1 = Api.builder().id(API_ID_1).build();
        Api api2 = Api.builder().id(API_ID_2).build();

        when(
            apiRepository.search(
                new ApiCriteria.Builder().state(LifecycleState.STARTED).environmentId(ENV_ID).build(),
                new ApiFieldFilter.Builder().excludeDefinition().excludePicture().build()
            )
        )
            .thenReturn(List.of(api1, api2));

        when(apiRepository.deleteByEnvironmentId(ENV_ID)).thenReturn(List.of(API_ID_1, API_ID_2));
        when(pageRepository.deleteByReferenceIdAndReferenceType(ENV_ID, PageReferenceType.ENVIRONMENT))
            .thenReturn(Map.of(ENV_ID, Collections.emptyList()));
        when(pageRepository.deleteByReferenceIdAndReferenceType(API_ID_1, PageReferenceType.API))
            .thenReturn(Map.of(API_ID_1, Collections.emptyList()));
        when(pageRepository.deleteByReferenceIdAndReferenceType(API_ID_2, PageReferenceType.API))
            .thenReturn(Map.of(API_ID_2, Collections.emptyList()));
        when(ratingRepository.deleteByReferenceIdAndReferenceType(API_ID_1, RatingReferenceType.API)).thenReturn(List.of(API_ID_1));
        when(ratingRepository.deleteByReferenceIdAndReferenceType(API_ID_2, RatingReferenceType.API)).thenReturn(List.of(API_ID_2));
        when(applicationRepository.deleteByEnvironmentId(ENV_ID)).thenReturn(List.of(APP_ID_1, APP_ID_2));
        when(planRepository.deleteByEnvironmentId(ENV_ID)).thenReturn(List.of(PLAN_ID_1, PLAN_ID_2));
        AlertTriggerEntity alert = mock(AlertTriggerEntity.class);
        when(alert.getId()).thenReturn(ALERT_ID_1);
        when(alertService.findByReference(AlertReferenceType.ENVIRONMENT, ENV_ID)).thenReturn(List.of(alert));
        when(groupRepository.deleteByEnvironmentId(ENV_ID)).thenReturn(List.of(GROUP_ID_1, GROUP_ID_2));
        when(subscriptionRepository.deleteByEnvironmentId(ENV_ID)).thenReturn(List.of(SUBSCRIPTION_ID_1, SUBSCRIPTION_ID_2));
        cut =
            new DeleteEnvironmentCommandHandler(
                accessPointRepository,
                apiCategoryOrderRepository,
                apiHeaderRepository,
                apiKeyRepository,
                apiQualityRuleRepository,
                apiRepository,
                applicationRepository,
                auditRepository,
                categoryRepository,
                commandRepository,
                customUserFieldsRepository,
                dashboardRepository,
                dictionaryRepository,
                flowRepository,
                genericNotificationConfigRepository,
                groupRepository,
                identityProviderActivationRepository,
                integrationRepository,
                invitationRepository,
                mediaRepository,
                membershipRepository,
                metadataRepository,
                pageRepository,
                pageRevisionRepository,
                parameterRepository,
                planRepository,
                portalNotificationConfigRepository,
                ratingAnswerRepository,
                ratingRepository,
                roleRepository,
                subscriptionRepository,
                themeRepository,
                workflowRepository,
                accessPointService,
                alertService,
                apiStateService,
                applicationAlertService,
                dictionaryService,
                environmentService,
                eventService,
                identityProviderActivationService,
                searchEngineService
            );
    }

    @Test
    public void should_delete_empty_environment() {
        DeleteEnvironmentReply reply = cut
            .handle(new DeleteEnvironmentCommand(new DeleteEnvironmentCommandPayload("delete-empty-env", EMPTY_ENV_ID, USER_ID)))
            .blockingGet();

        verify(environmentService).delete(EMPTY_ENV_ID);
        assertEquals(CommandStatus.SUCCEEDED, reply.getCommandStatus());
    }

    @Test
    public void should_reply_succeeded_if_environment_not_found() {
        DeleteEnvironmentReply reply = cut
            .handle(new DeleteEnvironmentCommand(new DeleteEnvironmentCommandPayload("delete-empty-env", NOT_FOUND_ENV_ID, USER_ID)))
            .blockingGet();

        assertEquals(CommandStatus.SUCCEEDED, reply.getCommandStatus());
    }

    @Test
    public void should_reply_error_if_throw_an_error() {
        DeleteEnvironmentReply reply = cut
            .handle(new DeleteEnvironmentCommand(new DeleteEnvironmentCommandPayload("delete-empty-env", ERROR_ENV_ID, USER_ID)))
            .blockingGet();

        assertEquals(CommandStatus.ERROR, reply.getCommandStatus());
    }

    @Test
    public void should_delete_environment() throws TechnicalException {
        DeleteEnvironmentReply reply = cut
            .handle(new DeleteEnvironmentCommand(new DeleteEnvironmentCommandPayload("delete-env", ENV_ID, USER_ID)))
            .blockingGet();

        ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        assertEquals(CommandStatus.SUCCEEDED, reply.getCommandStatus());
        verifyDisableEnvironment(executionContext);
        verifyDeleteApis(executionContext);
        verifyDeleteApplications(executionContext);
        verifyDeletePages(executionContext, PageReferenceType.ENVIRONMENT, ENV_ID);

        verify(subscriptionRepository).deleteByEnvironmentId(ENV_ID);
        verify(apiKeyRepository).deleteByEnvironmentId(ENV_ID);
        verify(planRepository).deleteByEnvironmentId(ENV_ID);
        verify(flowRepository).deleteByReferenceIdAndReferenceType(PLAN_ID_1, FlowReferenceType.PLAN);
        verify(flowRepository).deleteByReferenceIdAndReferenceType(PLAN_ID_2, FlowReferenceType.PLAN);
        verify(alertService).delete(ALERT_ID_1, ENV_ID);
        verify(apiHeaderRepository).deleteByEnvironmentId(ENV_ID);
        verify(accessPointRepository).deleteByReferenceIdAndReferenceType(ENV_ID, AccessPointReferenceType.ENVIRONMENT);
        verify(parameterRepository).deleteByReferenceIdAndReferenceType(ENV_ID, ParameterReferenceType.ENVIRONMENT);
        verify(auditRepository).deleteByReferenceIdAndReferenceType(ENV_ID, Audit.AuditReferenceType.ENVIRONMENT);
        verify(customUserFieldsRepository).deleteByReferenceIdAndReferenceType(ENV_ID, CustomUserFieldReferenceType.ENVIRONMENT);
        verify(groupRepository).deleteByEnvironmentId(ENV_ID);
        verify(integrationRepository).deleteByEnvironmentId(ENV_ID);
        verify(membershipRepository).deleteByReferenceIdAndReferenceType(GROUP_ID_1, MembershipReferenceType.GROUP);
        verify(invitationRepository).deleteByReferenceIdAndReferenceType(GROUP_ID_1, InvitationReferenceType.GROUP);
        verify(membershipRepository).deleteByReferenceIdAndReferenceType(GROUP_ID_2, MembershipReferenceType.GROUP);
        verify(invitationRepository).deleteByReferenceIdAndReferenceType(GROUP_ID_2, InvitationReferenceType.GROUP);
        verify(membershipRepository).deleteByReferenceIdAndReferenceType(ENV_ID, MembershipReferenceType.ENVIRONMENT);
        verify(roleRepository).deleteByReferenceIdAndReferenceType(ENV_ID, RoleReferenceType.ENVIRONMENT);
        verify(categoryRepository).deleteByEnvironmentId(ENV_ID);
        verify(dashboardRepository).deleteByReferenceIdAndReferenceType(ENV_ID, DashboardReferenceType.ENVIRONMENT);
        verify(dictionaryRepository).deleteByEnvironmentId(ENV_ID);
        verify(eventService).deleteOrUpdateEventsByEnvironment(ENV_ID);
        verify(portalNotificationConfigRepository)
            .deleteByReferenceIdAndReferenceType(PortalNotificationDefaultReferenceId.DEFAULT.name(), NotificationReferenceType.PORTAL);
        verify(genericNotificationConfigRepository).deleteByReferenceIdAndReferenceType(ENV_ID, NotificationReferenceType.PORTAL);
        verify(themeRepository).deleteByReferenceIdAndReferenceType(ENV_ID, ThemeReferenceType.ENVIRONMENT);
        verify(identityProviderActivationRepository)
            .deleteByReferenceIdAndReferenceType(
                ENV_ID,
                io.gravitee.repository.management.model.IdentityProviderActivationReferenceType.ENVIRONMENT
            );
        verify(commandRepository).deleteByEnvironmentId(ENV_ID);
        verify(mediaRepository).deleteByEnvironment(ENV_ID);
        verify(metadataRepository).deleteByReferenceIdAndReferenceType(ENV_ID, MetadataReferenceType.ENVIRONMENT);
        verify(environmentService).delete(ENV_ID);
    }

    private void verifyDeleteApplications(ExecutionContext executionContext) throws TechnicalException {
        verify(applicationRepository).deleteByEnvironmentId(executionContext.getEnvironmentId());
        verifyDeleteApplication(executionContext, APP_ID_1);
        verifyDeleteApplication(executionContext, APP_ID_2);
    }

    private void verifyDeleteApplication(ExecutionContext executionContext, String appId) throws TechnicalException {
        verify(genericNotificationConfigRepository).deleteByReferenceIdAndReferenceType(appId, NotificationReferenceType.APPLICATION);
        verify(membershipRepository).deleteByReferenceIdAndReferenceType(appId, MembershipReferenceType.APPLICATION);
        verify(applicationAlertService).deleteAll(appId);
        verify(portalNotificationConfigRepository).deleteByReferenceIdAndReferenceType(appId, NotificationReferenceType.APPLICATION);
        verify(auditRepository).deleteByReferenceIdAndReferenceType(appId, Audit.AuditReferenceType.APPLICATION);
        verify(metadataRepository).deleteByReferenceIdAndReferenceType(appId, MetadataReferenceType.APPLICATION);
        verify(invitationRepository).deleteByReferenceIdAndReferenceType(appId, InvitationReferenceType.APPLICATION);
        verify(workflowRepository).deleteByReferenceIdAndReferenceType(appId, Workflow.ReferenceType.APPLICATION.name());
    }

    private void verifyDeleteApis(ExecutionContext executionContext) throws TechnicalException {
        verify(apiRepository).deleteByEnvironmentId(executionContext.getEnvironmentId());
        verifyDeleteApi(executionContext, API_ID_1);
        verifyDeleteApi(executionContext, API_ID_2);
    }

    private void verifyDeleteApi(ExecutionContext executionContext, String apiId) throws TechnicalException {
        verify(membershipRepository).deleteByReferenceIdAndReferenceType(apiId, MembershipReferenceType.API);
        verify(genericNotificationConfigRepository).deleteByReferenceIdAndReferenceType(apiId, NotificationReferenceType.API);
        verify(eventService).deleteApiEvents(apiId);
        verify(searchEngineService).delete(executionContext, ApiEntity.builder().id(apiId).build());
        verifyDeletePages(executionContext, PageReferenceType.API, apiId);
        verify(portalNotificationConfigRepository).deleteByReferenceIdAndReferenceType(apiId, NotificationReferenceType.API);
        verify(apiCategoryOrderRepository).deleteByApiId(apiId);
        verify(apiQualityRuleRepository).deleteByApi(apiId);
        verify(mediaRepository).deleteAllByApi(apiId);
        verify(metadataRepository).deleteByReferenceIdAndReferenceType(apiId, MetadataReferenceType.API);
        verify(invitationRepository).deleteByReferenceIdAndReferenceType(apiId, InvitationReferenceType.API);
        verify(ratingRepository).deleteByReferenceIdAndReferenceType(apiId, RatingReferenceType.API);
        verify(ratingAnswerRepository).deleteByRating(apiId);
    }

    private void verifyDeletePages(ExecutionContext executionContext, PageReferenceType type, String id) throws TechnicalException {
        verify(pageRepository).deleteByReferenceIdAndReferenceType(id, type);
        verify(searchEngineService).delete(executionContext, PageEntity.builder().id(id).build());
        verify(pageRevisionRepository).deleteByPageId(id);
    }

    private void verifyDisableEnvironment(ExecutionContext executionContext) {
        verify(apiStateService).stop(executionContext, API_ID_1, USER_ID);
        verify(apiStateService).stop(executionContext, API_ID_2, USER_ID);
        verify(dictionaryService).stop(executionContext, DICTIONARY_ID);
        verify(identityProviderActivationService)
            .removeAllIdpsFromTarget(
                executionContext,
                new IdentityProviderActivationService.ActivationTarget(
                    executionContext.getEnvironmentId(),
                    IdentityProviderActivationReferenceType.ENVIRONMENT
                )
            );
    }

    @Test
    public void should_support_type() {
        assertEquals(CockpitCommandType.DELETE_ENVIRONMENT.name(), cut.supportType());
    }
}
