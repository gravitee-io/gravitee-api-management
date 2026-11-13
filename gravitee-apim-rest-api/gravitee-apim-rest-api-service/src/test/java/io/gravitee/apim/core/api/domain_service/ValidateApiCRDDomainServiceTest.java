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
package io.gravitee.apim.core.api.domain_service;

import static fixtures.core.model.ApiCRDFixtures.API_CROSS_ID;
import static fixtures.core.model.ApiCRDFixtures.API_HRID;
import static io.gravitee.apim.core.group.model.Group.GroupEvent.API_CREATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiCRDFixtures;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.CRDMembersDomainServiceInMemory;
import inmemory.ClusterQueryServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.exception.ApiPropertyEncryptedToPlainException;
import io.gravitee.apim.core.api.exception.ApiPropertyNotCiphertextException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBindingPermissionDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterResolutionDomainService;
import io.gravitee.apim.core.cluster.domain_service.DeployedClustersDomainService;
import io.gravitee.apim.core.cluster.domain_service.ValidateApiClusterBindingService;
import io.gravitee.apim.core.cluster.domain_service.ValidateKafkaClusterBindingDomainService;
import io.gravitee.apim.core.cluster.exception.ClusterBindingNotAllowedException;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.flow.exception.InvalidFlowException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.notification.domain_service.ValidatePortalNotificationDomainService;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.plan.domain_service.ValidatePlanDomainService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateApiCRDDomainServiceTest {

    private static final String ORG_ID = "TEST";
    private static final String ENV_ID = "TEST";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORG_ID, ENV_ID, USER_ID);
    private static final ObjectMapper OBJECT_MAPPER = new GraviteeMapper();

    ValidateCategoryIdsDomainService categoryIdsValidator = mock(ValidateCategoryIdsDomainService.class);

    VerifyApiPathDomainService pathValidator = mock(VerifyApiPathDomainService.class);

    VerifyApiHostsDomainService apiHostValidator = mock(VerifyApiHostsDomainService.class);

    ValidateCRDMembersDomainService membersValidator = mock(ValidateCRDMembersDomainService.class);

    ValidateGroupsDomainService groupsValidator = mock(ValidateGroupsDomainService.class);

    ValidateResourceDomainService resourceValidator = mock(ValidateResourceDomainService.class);

    ValidatePagesDomainService pagesValidator = mock(ValidatePagesDomainService.class);

    ValidatePlanDomainService planValidator = mock(ValidatePlanDomainService.class);

    ValidatePortalNotificationDomainService portalNotificationValidator = mock(ValidatePortalNotificationDomainService.class);

    FlowValidationDomainService flowValidator = mock(FlowValidationDomainService.class);

    UpdateApiDomainService updateApiDomainService = mock(UpdateApiDomainService.class);

    ValidateHealthCheckScheduleDomainService healthCheckScheduleValidator = new ValidateHealthCheckScheduleDomainService(
        new ObjectMapper()
    );

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);

    DataEncryptor dataEncryptor = mock(DataEncryptor.class);

    CRDMembersDomainServiceInMemory membersDomainService = new CRDMembersDomainServiceInMemory();

    ClusterQueryServiceInMemory clusterQueryService = new ClusterQueryServiceInMemory();

    PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);

    EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();

    ValidateApiCRDDomainService cut = new ValidateApiCRDDomainService(
        categoryIdsValidator,
        pathValidator,
        apiHostValidator,
        membersValidator,
        groupsValidator,
        resourceValidator,
        pagesValidator,
        planValidator,
        portalNotificationValidator,
        healthCheckScheduleValidator,
        new ApiCRDTargetDomainService(apiQueryService, apiCrudService),
        new PropertyDomainService(dataEncryptor),
        membersDomainService,
        new ValidateKafkaClusterBindingDomainService(
            new DeployedClustersDomainService(eventLatestQueryService, OBJECT_MAPPER),
            OBJECT_MAPPER
        ),
        new ClusterBindingPermissionDomainService(clusterQueryService, permissionDomainService, new ObjectMapper()),
        new ValidateApiClusterBindingService(
            new ClusterResolutionDomainService(
                clusterQueryService,
                new DeployedClustersDomainService(eventLatestQueryService, OBJECT_MAPPER),
                OBJECT_MAPPER
            ),
            new ValidateKafkaClusterBindingDomainService(
                new DeployedClustersDomainService(eventLatestQueryService, OBJECT_MAPPER),
                OBJECT_MAPPER
            ),
            OBJECT_MAPPER
        ),
        mock(ApiPrimaryOwnerFactory.class),
        mock(ValidateApiDomainService.class),
        updateApiDomainService,
        flowValidator,
        new PlanQueryServiceInMemory(),
        mock(ApiPrimaryOwnerDomainService.class),
        mock(ApiFolderConflictsDomainService.class)
    );

    PortalNotificationConfigEntity consoleNotificationConfiguration = new PortalNotificationConfigEntity();

    @BeforeEach
    void setUp() {
        reset(categoryIdsValidator);
        when(pathValidator.validateAndSanitize(any())).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));
        lenient()
            .when(flowValidator.validateAndSanitizeHttpV4(any(), any(), any()))
            .thenAnswer(call -> call.getArgument(1));
        consoleNotificationConfiguration.setConfigType(NotificationConfigType.PORTAL);
    }

    @Test
    void should_reject_making_an_encrypted_property_of_the_existing_api_plain() {
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).crossId(API_CROSS_ID).build();
        existingApi
            .getApiDefinitionHttpV4()
            .setProperties(List.of(Property.builder().key("secret").value("ciphertext").encrypted(true).build()));
        apiQueryService.initWith(List.of(existingApi));
        var spec = ApiCRDFixtures.newBaseSpec()
            .id(existingApi.getId())
            .crossId(API_CROSS_ID)
            .properties(List.of(EncryptableProperty.builder().key("secret").value("ciphertext").build()))
            .build();

        assertThatExceptionOfType(ApiPropertyEncryptedToPlainException.class).isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec))
        );
    }

    @Test
    void should_reject_a_changed_plaintext_flagged_encrypted_on_the_existing_api() throws GeneralSecurityException {
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).crossId(API_CROSS_ID).build();
        existingApi
            .getApiDefinitionHttpV4()
            .setProperties(List.of(Property.builder().key("secret").value("ciphertext").encrypted(true).build()));
        apiQueryService.initWith(List.of(existingApi));
        when(dataEncryptor.decrypt("n3w-plaintext")).thenThrow(new GeneralSecurityException("bad padding"));
        var spec = ApiCRDFixtures.newBaseSpec()
            .id(existingApi.getId())
            .crossId(API_CROSS_ID)
            .properties(List.of(EncryptableProperty.builder().key("secret").value("n3w-plaintext").encrypted(true).build()))
            .build();

        assertThatExceptionOfType(ApiPropertyNotCiphertextException.class).isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec))
        );
    }

    @Test
    void should_reject_an_id_that_differs_from_the_existing_api() {
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id("existing-api-id").environmentId(ENV_ID).crossId(API_CROSS_ID).build();
        apiQueryService.initWith(List.of(existingApi));
        var spec = ApiCRDFixtures.newBaseSpec().id("another-api-id").crossId(API_CROSS_ID).build();

        assertThatExceptionOfType(ValidationDomainException.class).isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec))
        );
        verifyNoInteractions(categoryIdsValidator, pathValidator, planValidator);
    }

    @Test
    void should_reject_a_member_change_on_the_existing_api_without_the_member_permission() {
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().environmentId(ENV_ID).crossId(API_CROSS_ID).build();
        apiQueryService.initWith(List.of(existingApi));
        membersDomainService.denyApiMembersUpdate(existingApi.getId());
        givenEveryValidatorAccepts();
        var spec = ApiCRDFixtures.newBaseSpec()
            .id(existingApi.getId())
            .crossId(API_CROSS_ID)
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .build();

        assertThatExceptionOfType(ForbiddenAccessException.class).isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec))
        );
    }

    @Test
    void should_not_check_the_members_of_an_api_to_create() {
        membersDomainService.denyApiMembersUpdate("api-to-create");
        givenEveryValidatorAccepts();
        // A spec the import can create: the validation of its writes runs too
        var spec = ApiCRDFixtures.newBaseSpec()
            .id("api-to-create")
            .crossId("api-to-create-cross-id")
            .type("PROXY")
            .lifecycleState("CREATED")
            .visibility("PRIVATE")
            .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("KUBERNETES").build())
            .properties(List.of())
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .build();

        assertThatNoException().isThrownBy(() -> cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec)));
    }

    @Test
    void should_refuse_an_id_that_names_an_api_of_another_environment() {
        var foreignApi = ApiFixtures.aProxyApiV4().toBuilder().environmentId("other-environment").crossId(API_CROSS_ID).build();
        apiQueryService.initWith(List.of(foreignApi));
        givenEveryValidatorAccepts();
        var spec = ApiCRDFixtures.newBaseSpec()
            .id(foreignApi.getId())
            .crossId(API_CROSS_ID)
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .build();

        assertThatExceptionOfType(ValidationDomainException.class)
            .isThrownBy(() -> cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec)))
            .withMessageContaining(foreignApi.getId())
            .withMessageContaining("another environment");
    }

    @Test
    void should_refuse_a_binding_to_a_cluster_the_user_is_not_a_member_of() {
        givenAClusterTheUserIsNotAMemberOf();
        givenEveryValidatorAccepts();
        var spec = aNativeSpecBoundTo("plain");

        assertThatExceptionOfType(ClusterBindingNotAllowedException.class)
            .isThrownBy(() -> cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, spec)))
            .withMessageContaining("[Production]");
    }

    @Test
    void should_refuse_a_message_api_binding_to_a_cluster_the_user_is_not_a_member_of() {
        givenAClusterTheUserIsNotAMemberOf();
        givenTheClusterIsDeployed();
        givenEveryValidatorAccepts();

        assertThatExceptionOfType(ClusterBindingNotAllowedException.class).isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, aMessageSpecBoundTo("plain")))
        );
    }

    @Test
    void should_accept_a_message_api_bound_to_a_connection_of_a_deployed_cluster() {
        givenAClusterTheUserIsNotAMemberOf();
        givenTheClusterIsDeployed();
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

        assertThat(severeErrorsOf(systemCall, aMessageSpecBoundTo("plain"))).isEmpty();
    }

    @Test
    void should_report_a_message_api_bound_to_a_cluster_that_is_not_deployed() {
        givenAClusterTheUserIsNotAMemberOf();
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

        assertThat(severeErrorsOf(systemCall, aMessageSpecBoundTo("plain")))
            .singleElement(STRING)
            .contains("Kafka cluster [prod] is not deployed in this environment");
    }

    @Test
    void should_report_a_message_api_bound_to_a_connection_the_deployed_cluster_does_not_have() {
        givenAClusterTheUserIsNotAMemberOf();
        givenTheClusterIsDeployed();
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

        assertThat(severeErrorsOf(systemCall, aMessageSpecBoundTo("unknown")))
            .singleElement(STRING)
            .contains("connection [unknown] does not exist in the deployed Kafka cluster [prod]");
    }

    @Test
    void should_let_the_existing_api_keep_its_binding_to_a_cluster_the_user_is_not_a_member_of() {
        givenAClusterTheUserIsNotAMemberOf();
        givenEveryValidatorAccepts();
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aNativeApiBoundToCluster("api-id", ENV_ID, Api.LifecycleState.STARTED, "prod", "plain")
                    .toBuilder()
                    .crossId(API_CROSS_ID)
                    .build()
            )
        );

        assertThatNoException().isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, aNativeSpecBoundTo("plain")))
        );
        assertThatExceptionOfType(ClusterBindingNotAllowedException.class).isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(AUDIT_INFO, aNativeSpecBoundTo("sasl")))
        );
    }

    @Test
    void should_report_a_started_native_api_bound_to_a_cluster_that_is_not_deployed() {
        givenTheClusterIsUndeployed();
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

        assertThat(severeErrorsOf(systemCall, aNativeSpecBoundTo("plain")))
            .singleElement(STRING)
            .contains("its cluster {prod} is not deployed");
    }

    @Test
    void should_accept_a_stopped_native_api_bound_to_a_cluster_that_is_not_deployed() {
        givenTheClusterIsUndeployed();
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        var spec = aNativeSpecBoundTo("plain").toBuilder().state("STOPPED").build();

        assertThat(severeErrorsOf(systemCall, spec)).isEmpty();
    }

    @Test
    void should_report_a_started_message_api_bound_to_an_undeployed_cluster_whose_last_deployment_remains() {
        givenTheClusterIsUndeployed();
        givenTheClusterIsDeployed();
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

        assertThat(severeErrorsOf(systemCall, aMessageSpecBoundTo("plain")))
            .singleElement(STRING)
            .contains("its cluster {prod} is not deployed");
    }

    @Test
    void should_report_a_state_other_than_started_or_stopped() {
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        var spec = aNativeSpecBoundTo("plain").toBuilder().state("PAUSED").build();

        assertThat(severeErrorsOf(systemCall, spec)).singleElement(STRING).contains("State [PAUSED] is invalid");
    }

    @Test
    void should_report_a_state_in_lower_case() {
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        var spec = aNativeSpecBoundTo("plain").toBuilder().state("started").build();

        assertThat(severeErrorsOf(systemCall, spec)).anySatisfy(error -> assertThat(error).contains("State [started] is invalid"));
    }

    @Test
    void should_report_a_stored_started_native_api_bound_to_a_cluster_that_is_not_deployed_when_the_spec_has_no_state() {
        givenTheClusterIsUndeployed();
        givenEveryValidatorAccepts();
        givenAStoredNativeApi(Api.LifecycleState.STARTED);
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        var spec = aNativeSpecBoundTo("plain").toBuilder().state(null).build();

        assertThat(severeErrorsOf(systemCall, spec)).singleElement(STRING).contains("its cluster {prod} is not deployed");
    }

    @Test
    void should_accept_a_stored_stopped_native_api_bound_to_a_cluster_that_is_not_deployed_when_the_spec_has_no_state() {
        givenTheClusterIsUndeployed();
        givenEveryValidatorAccepts();
        givenAStoredNativeApi(Api.LifecycleState.STOPPED);
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        var spec = aNativeSpecBoundTo("plain").toBuilder().state(null).build();

        assertThat(severeErrorsOf(systemCall, spec)).isEmpty();
    }

    @Test
    void should_accept_a_stopped_spec_of_a_stored_started_native_api_bound_to_a_cluster_that_is_not_deployed() {
        givenTheClusterIsUndeployed();
        givenEveryValidatorAccepts();
        givenAStoredNativeApi(Api.LifecycleState.STARTED);
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        var spec = aNativeSpecBoundTo("plain").toBuilder().state("STOPPED").build();

        assertThat(severeErrorsOf(systemCall, spec)).isEmpty();
    }

    @Test
    void should_accept_a_spec_without_state() {
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();
        var spec = aNativeSpecBoundTo("plain").toBuilder().state(null).build();

        assertThat(severeErrorsOf(systemCall, spec)).isEmpty();
    }

    @Test
    void should_not_check_the_bindings_without_an_actor() {
        givenAClusterTheUserIsNotAMemberOf();
        givenEveryValidatorAccepts();
        var systemCall = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

        assertThatNoException().isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApiCRDDomainService.Input(systemCall, aNativeSpecBoundTo("plain")))
        );
    }

    private void givenAClusterTheUserIsNotAMemberOf() {
        clusterQueryService.initWith(
            List.of(
                Cluster.builder()
                    .id("prod-id")
                    .crossId("prod")
                    .name("Production")
                    .type(ClusterType.KAFKA_CLUSTER)
                    .environmentId(ENV_ID)
                    .lifecycleState(ClusterLifecycleState.DEPLOYED)
                    .build()
            )
        );
    }

    private void givenTheClusterIsUndeployed() {
        clusterQueryService.initWith(
            List.of(
                Cluster.builder()
                    .id("prod-id")
                    .crossId("prod")
                    .name("Production")
                    .type(ClusterType.KAFKA_CLUSTER)
                    .environmentId(ENV_ID)
                    .lifecycleState(ClusterLifecycleState.UNDEPLOYED)
                    .build()
            )
        );
    }

    private void givenAStoredNativeApi(Api.LifecycleState lifecycleState) {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aNativeApiBoundToCluster("api-id", ENV_ID, lifecycleState, "prod", "plain")
                    .toBuilder()
                    .crossId(API_CROSS_ID)
                    .build()
            )
        );
    }

    private void givenTheClusterIsDeployed() {
        var cluster = Cluster.builder()
            .id("prod-id")
            .crossId("prod")
            .name("Production")
            .type(ClusterType.KAFKA_CLUSTER)
            .environmentId(ENV_ID)
            .configuration(
                Map.of(
                    "connections",
                    List.of(
                        Map.of(
                            "crossId",
                            "plain",
                            "name",
                            "Plain",
                            "bootstrapServers",
                            "kafka:9092",
                            "security",
                            Map.of("protocol", "PLAINTEXT")
                        )
                    )
                )
            )
            .build();
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, cluster.getCrossId());
        try {
            eventLatestQueryService.initWith(
                List.of(
                    Event.builder()
                        .id("deploy-prod")
                        .type(EventType.DEPLOY_CLUSTER)
                        .payload(OBJECT_MAPPER.writeValueAsString(cluster))
                        .environments(Set.of(ENV_ID))
                        .properties(properties)
                        .createdAt(ZonedDateTime.now())
                        .build()
                )
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> severeErrorsOf(AuditInfo auditInfo, ApiCRDSpec spec) {
        return cut
            .validateAndSanitize(new ValidateApiCRDDomainService.Input(auditInfo, spec))
            .severe()
            .orElse(List.of())
            .stream()
            .map(Validator.Error::getMessage)
            .toList();
    }

    private ApiCRDSpec aMessageSpecBoundTo(String connectionCrossId) {
        var definition = ApiFixtures.aMessageApiBoundToCluster(
            "api-id",
            ENV_ID,
            Api.LifecycleState.STARTED,
            "prod",
            connectionCrossId
        ).getApiDefinitionHttpV4();
        return ApiCRDFixtures.newBaseSpec()
            .type("MESSAGE")
            .endpointGroups(definition.getEndpointGroups())
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .build();
    }

    private ApiCRDSpec aNativeSpecBoundTo(String connectionCrossId) {
        var definition = ApiFixtures.aNativeApiBoundToCluster(
            "api-id",
            ENV_ID,
            Api.LifecycleState.STARTED,
            "prod",
            connectionCrossId
        ).getApiDefinitionNativeV4();
        // A spec the import can update: the validation of its writes runs too
        return ApiCRDFixtures.newBaseNativeSpec()
            .id("api-id")
            .lifecycleState("CREATED")
            .visibility("PRIVATE")
            .endpointGroups(definition.getEndpointGroups())
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .build();
    }

    private void givenEveryValidatorAccepts() {
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

    @Test
    void should_preserve_pre_set_ids() {
        var spec = ApiCRDFixtures.newBaseSpec()
            .id("pre-set-id")
            .crossId("pre-set-cross-id")
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .build();
        var input = new ValidateApiCRDDomainService.Input(AuditInfo.builder().environmentId(ENV_ID).organizationId(ORG_ID).build(), spec);

        when(categoryIdsValidator.validateAndSanitize(new ValidateCategoryIdsDomainService.Input(ENV_ID, spec.getCategories()))).thenAnswer(
            call -> Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            membersValidator.validateAndSanitize(
                new ValidateCRDMembersDomainService.Input(AUDIT_INFO, MembershipReferenceType.APPLICATION, any())
            )
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(
            groupsValidator.validateAndSanitize(new ValidateGroupsDomainService.Input(ENV_ID, any(), null, null, API_CREATE, true))
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(resourceValidator.validateAndSanitize(new ValidateResourceDomainService.Input(ENV_ID, any()))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            pagesValidator.validateAndSanitize(new ValidatePagesDomainService.Input(AUDIT_INFO, spec.getId(), spec.getHrid(), any()))
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(planValidator.validateAndSanitize(any(ValidatePlanDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            portalNotificationValidator.validateAndSanitize(
                new ValidatePortalNotificationDomainService.Input(consoleNotificationConfiguration, any(), null, null, AUDIT_INFO)
            )
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        cut
            .validateAndSanitize(input)
            .peek(
                sanitized -> {
                    Assertions.assertThat(sanitized.spec().getId()).isEqualTo("pre-set-id");
                    Assertions.assertThat(sanitized.spec().getCrossId()).isEqualTo("pre-set-cross-id");
                },
                errors -> Assertions.assertThat(errors).isEmpty()
            );
    }

    @Test
    void should_return_input_with_categories_and_no_warnings() {
        var spec = ApiCRDFixtures.newBaseSpec()
            .categories(Set.of("key-1", "id-2"))
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .build();
        var input = new ValidateApiCRDDomainService.Input(AuditInfo.builder().environmentId(ENV_ID).organizationId(ORG_ID).build(), spec);

        when(categoryIdsValidator.validateAndSanitize(new ValidateCategoryIdsDomainService.Input(ENV_ID, spec.getCategories()))).thenReturn(
            Validator.Result.ofValue(new ValidateCategoryIdsDomainService.Input(ENV_ID, Set.of("id-1", "id-2")))
        );

        when(
            membersValidator.validateAndSanitize(
                new ValidateCRDMembersDomainService.Input(AUDIT_INFO, MembershipReferenceType.APPLICATION, any())
            )
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(
            groupsValidator.validateAndSanitize(new ValidateGroupsDomainService.Input(ENV_ID, any(), null, null, API_CREATE, true))
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(resourceValidator.validateAndSanitize(new ValidateResourceDomainService.Input(ENV_ID, any()))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            pagesValidator.validateAndSanitize(new ValidatePagesDomainService.Input(AUDIT_INFO, spec.getId(), spec.getHrid(), any()))
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(planValidator.validateAndSanitize(any(ValidatePlanDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            portalNotificationValidator.validateAndSanitize(
                new ValidatePortalNotificationDomainService.Input(consoleNotificationConfiguration, any(), null, null, AUDIT_INFO)
            )
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        cut
            .validateAndSanitize(input)
            .peek(
                sanitized -> {
                    var expected = spec.toBuilder().crossId(API_CROSS_ID).hrid(API_HRID).categories(Set.of("id-1", "id-2")).build();
                    Assertions.assertThat(sanitized.spec()).isEqualTo(expected);
                },
                errors -> Assertions.assertThat(errors).isEmpty()
            );
    }

    @Test
    void should_return_input_with_the_host_no_errors() {
        var spec = ApiCRDFixtures.newBaseNativeSpec().build();
        var input = new ValidateApiCRDDomainService.Input(AuditInfo.builder().environmentId(ENV_ID).build(), spec);

        when(apiHostValidator.checkApiHosts(any(), any(), any(), any())).thenReturn(true);

        when(categoryIdsValidator.validateAndSanitize(new ValidateCategoryIdsDomainService.Input(ENV_ID, spec.getCategories()))).thenAnswer(
            call -> Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            membersValidator.validateAndSanitize(
                new ValidateCRDMembersDomainService.Input(AUDIT_INFO, MembershipReferenceType.APPLICATION, any())
            )
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(
            groupsValidator.validateAndSanitize(new ValidateGroupsDomainService.Input(ENV_ID, any(), null, null, API_CREATE, true))
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(resourceValidator.validateAndSanitize(new ValidateResourceDomainService.Input(ENV_ID, any()))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            pagesValidator.validateAndSanitize(new ValidatePagesDomainService.Input(AUDIT_INFO, spec.getId(), spec.getHrid(), any()))
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(planValidator.validateAndSanitize(any(ValidatePlanDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            portalNotificationValidator.validateAndSanitize(
                new ValidatePortalNotificationDomainService.Input(consoleNotificationConfiguration, any(), null, null, AUDIT_INFO)
            )
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        var expected = spec.toBuilder().crossId(API_CROSS_ID).hrid(API_HRID).build();

        cut
            .validateAndSanitize(input)
            .peek(
                sanitized -> Assertions.assertThat(sanitized.spec()).isEqualTo(expected),
                errors -> Assertions.assertThat(errors).isEmpty()
            );
    }

    @Test
    void should_return_severe_error_for_invalid_health_check_schedule() {
        var group = (EndpointGroup) ApiCRDFixtures.newBaseSpec().build().getEndpointGroups().getFirst();
        group.setServices(
            EndpointGroupServices.builder()
                .healthCheck(
                    Service.builder()
                        .type(ValidateHealthCheckScheduleDomainService.HTTP_HEALTH_CHECK_TYPE)
                        .enabled(true)
                        .overrideConfiguration(false)
                        .configuration("{\"schedule\":\"*/30 * * * *\"}")
                        .build()
                )
                .build()
        );
        var spec = ApiCRDFixtures.newBaseSpec()
            .endpointGroups(List.of(group))
            .consoleNotificationConfiguration(consoleNotificationConfiguration)
            .build();
        var input = new ValidateApiCRDDomainService.Input(AuditInfo.builder().environmentId(ENV_ID).organizationId(ORG_ID).build(), spec);

        when(categoryIdsValidator.validateAndSanitize(new ValidateCategoryIdsDomainService.Input(ENV_ID, spec.getCategories()))).thenAnswer(
            call -> Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            membersValidator.validateAndSanitize(
                new ValidateCRDMembersDomainService.Input(AUDIT_INFO, MembershipReferenceType.APPLICATION, any())
            )
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(
            groupsValidator.validateAndSanitize(new ValidateGroupsDomainService.Input(ENV_ID, any(), null, null, API_CREATE, true))
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(resourceValidator.validateAndSanitize(new ValidateResourceDomainService.Input(ENV_ID, any()))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            pagesValidator.validateAndSanitize(new ValidatePagesDomainService.Input(AUDIT_INFO, spec.getId(), spec.getHrid(), any()))
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        when(planValidator.validateAndSanitize(any(ValidatePlanDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        when(
            portalNotificationValidator.validateAndSanitize(
                new ValidatePortalNotificationDomainService.Input(consoleNotificationConfiguration, any(), null, null, AUDIT_INFO)
            )
        ).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

        var severeErrors = cut.validateAndSanitize(input).severe();

        assertThat(severeErrors).isPresent();
        assertThat(severeErrors.get()).hasSize(1);
        assertThat(severeErrors.get().getFirst().isSevere()).isTrue();
        assertThat(severeErrors.get().getFirst().getMessage())
            .contains("*/30 * * * *")
            .contains("6 fields")
            .contains("endpointGroups[default-group].services.healthCheck.configuration.schedule");
    }

    @Test
    void should_reject_api_flow_with_null_path_operator() {
        var corruptFlow = Flow.builder()
            .name("api-flow")
            .selectors(List.of(HttpSelector.builder().path("/calls").pathOperator(null).build()))
            .build();
        var spec = ApiCRDFixtures.newBaseSpec().flows(List.of(corruptFlow)).build();
        var input = new ValidateApiCRDDomainService.Input(AuditInfo.builder().environmentId(ENV_ID).build(), spec);

        var throwable = Assertions.catchThrowable(() -> cut.validateAndSanitize(input));

        Assertions.assertThat(throwable)
            .isInstanceOf(InvalidFlowException.class)
            .hasMessage("The flow [api-flow] contains an HTTP selector with a missing pathOperator");
        Assertions.assertThat(((InvalidFlowException) throwable).getParameters()).containsEntry("flowName", "api-flow");
    }
}
