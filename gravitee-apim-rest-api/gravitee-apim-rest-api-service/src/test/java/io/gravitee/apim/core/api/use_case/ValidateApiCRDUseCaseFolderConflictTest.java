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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiCRDFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.CRDMembersDomainServiceInMemory;
import inmemory.ClusterQueryServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiCRDTargetDomainService;
import io.gravitee.apim.core.api.domain_service.ApiFolderConflictsDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateHealthCheckScheduleDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBindingPermissionDomainService;
import io.gravitee.apim.core.cluster.domain_service.DeployedClustersDomainService;
import io.gravitee.apim.core.cluster.domain_service.ValidateApiClusterBindingService;
import io.gravitee.apim.core.cluster.domain_service.ValidateKafkaClusterBindingDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.notification.domain_service.ValidatePortalNotificationDomainService;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.plan.domain_service.ValidatePlanDomainService;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The Management API v2 dry run of an API CRD must refuse the folder conflicts the import refuses:
 * a portal folder nobody automated sitting at a path the spec declares.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateApiCRDUseCaseFolderConflictTest {

    private static final String ORG_ID = "TEST";
    private static final String ENV_ID = "TEST";
    private static final String USER_ID = "user-id";
    private static final String API_ID = "api-to-create";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORG_ID, ENV_ID, USER_ID);
    private static final ObjectMapper OBJECT_MAPPER = new GraviteeMapper();

    ValidateCategoryIdsDomainService categoryIdsValidator = mock(ValidateCategoryIdsDomainService.class);

    VerifyApiPathDomainService pathValidator = mock(VerifyApiPathDomainService.class);

    ValidateCRDMembersDomainService membersValidator = mock(ValidateCRDMembersDomainService.class);

    ValidateGroupsDomainService groupsValidator = mock(ValidateGroupsDomainService.class);

    ValidateResourceDomainService resourceValidator = mock(ValidateResourceDomainService.class);

    ValidatePagesDomainService pagesValidator = mock(ValidatePagesDomainService.class);

    ValidatePlanDomainService planValidator = mock(ValidatePlanDomainService.class);

    ValidatePortalNotificationDomainService portalNotificationValidator = mock(ValidatePortalNotificationDomainService.class);

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);

    ClusterQueryServiceInMemory clusterQueryService = new ClusterQueryServiceInMemory();

    EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();

    ApiFolderConflictsDomainService apiFolderConflictsValidator = mock(ApiFolderConflictsDomainService.class);

    ValidateApiCRDDomainService crdValidator = new ValidateApiCRDDomainService(
        categoryIdsValidator,
        pathValidator,
        mock(VerifyApiHostsDomainService.class),
        membersValidator,
        groupsValidator,
        resourceValidator,
        pagesValidator,
        planValidator,
        portalNotificationValidator,
        new ValidateHealthCheckScheduleDomainService(OBJECT_MAPPER),
        new ApiCRDTargetDomainService(apiQueryService, apiCrudService),
        new PropertyDomainService(mock(DataEncryptor.class)),
        new CRDMembersDomainServiceInMemory(),
        new ValidateKafkaClusterBindingDomainService(
            new DeployedClustersDomainService(eventLatestQueryService, OBJECT_MAPPER),
            OBJECT_MAPPER
        ),
        new ClusterBindingPermissionDomainService(clusterQueryService, mock(PermissionDomainService.class), OBJECT_MAPPER),
        new ValidateApiClusterBindingService(clusterQueryService, OBJECT_MAPPER),
        mock(ApiPrimaryOwnerFactory.class),
        mock(ValidateApiDomainService.class),
        mock(UpdateApiDomainService.class),
        mock(FlowValidationDomainService.class),
        new PlanQueryServiceInMemory(),
        mock(ApiPrimaryOwnerDomainService.class),
        apiFolderConflictsValidator
    );

    ValidateApiCRDUseCase cut = new ValidateApiCRDUseCase(crdValidator);

    PortalNotificationConfigEntity consoleNotificationConfiguration = new PortalNotificationConfigEntity();

    @BeforeEach
    void setUp() {
        consoleNotificationConfiguration.setConfigType(NotificationConfigType.PORTAL);
        givenEveryValidatorAccepts();
    }

    @Test
    void should_refuse_a_hand_made_folder_at_a_declared_path_naming_the_path() {
        var navigation = List.of(new NavigationPath("/tba-guides", null));
        var spec = aSpecNavigating(navigation);
        givenAHandMadeFolderAt("/tba-guides", spec);

        assertThat(dryRunSevereErrors(spec)).containsExactly(
            "Navigation path [/tba-guides] is already occupied by content not managed by the Automation API"
        );
        assertThat(apiCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_a_hand_made_folder_nested_under_a_managed_folder_naming_the_path() {
        var navigation = List.of(new NavigationPath("/apis/echo", null), new NavigationPath("/apis/echo/tba-guides", null));
        var spec = aSpecNavigating(navigation);
        givenAHandMadeFolderAt("/apis/echo/tba-guides", spec);

        assertThat(dryRunSevereErrors(spec)).containsExactly(
            "Navigation path [/apis/echo/tba-guides] is already occupied by content not managed by the Automation API"
        );
        assertThat(apiCrudService.storage()).isEmpty();
    }

    private List<String> dryRunSevereErrors(ApiCRDSpec spec) {
        var errors = cut.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, spec)).status().getErrors();
        return errors == null ? List.of() : errors.severe();
    }

    private void givenAHandMadeFolderAt(String path, ApiCRDSpec spec) {
        doThrow(PathConflictException.folderPath(path))
            .when(apiFolderConflictsValidator)
            .validateApiFolderConflictsForApi(AUDIT_INFO, spec.getId(), spec.getPortalNavigation());
    }

    private ApiCRDSpec aSpecNavigating(List<NavigationPath> navigation) {
        return ApiCRDFixtures.newBaseSpec()
            .id(API_ID)
            .crossId("api-to-create-cross-id")
            .type("PROXY")
            .lifecycleState("CREATED")
            .visibility("PRIVATE")
            .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("KUBERNETES").build())
            .properties(List.of())
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .portalNavigation(navigation)
            .build();
    }

    private void givenEveryValidatorAccepts() {
        when(pathValidator.validateAndSanitize(any())).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));
        when(categoryIdsValidator.validateAndSanitize(any(ValidateCategoryIdsDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(membersValidator.validateAndSanitize(any(ValidateCRDMembersDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(groupsValidator.validateAndSanitize(any(ValidateGroupsDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(resourceValidator.validateAndSanitize(any(ValidateResourceDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(pagesValidator.validateAndSanitize(any(ValidatePagesDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(planValidator.validateAndSanitize(any(ValidatePlanDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(portalNotificationValidator.validateAndSanitize(any(ValidatePortalNotificationDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
    }
}
