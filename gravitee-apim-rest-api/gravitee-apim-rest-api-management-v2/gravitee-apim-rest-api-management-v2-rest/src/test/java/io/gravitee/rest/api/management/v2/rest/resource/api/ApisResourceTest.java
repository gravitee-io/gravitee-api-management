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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.core.model.ApiFixtures.aTcpApiV4;
import static io.gravitee.apim.core.member.model.SystemRole.PRIMARY_OWNER;
import static io.gravitee.common.http.HttpStatusCode.ACCEPTED_202;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.ApiQueryServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.ValidateResourceDomainServiceInMemory;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.exception.NativeApiWithMultipleFlowsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.management.v2.rest.model.Analytics;
import io.gravitee.rest.api.management.v2.rest.model.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.BaseSelector;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiV4;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV4;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.Entrypoint;
import io.gravitee.rest.api.management.v2.rest.model.FlowExecution;
import io.gravitee.rest.api.management.v2.rest.model.FlowMode;
import io.gravitee.rest.api.management.v2.rest.model.FlowV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpListener;
import io.gravitee.rest.api.management.v2.rest.model.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.model.HttpSelector;
import io.gravitee.rest.api.management.v2.rest.model.KafkaListener;
import io.gravitee.rest.api.management.v2.rest.model.Listener;
import io.gravitee.rest.api.management.v2.rest.model.ListenerType;
import io.gravitee.rest.api.management.v2.rest.model.Operator;
import io.gravitee.rest.api.management.v2.rest.model.PathV4;
import io.gravitee.rest.api.management.v2.rest.model.Qos;
import io.gravitee.rest.api.management.v2.rest.model.Selector;
import io.gravitee.rest.api.management.v2.rest.model.StepV4;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiHosts;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiHostsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApisResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Autowired
    private ApiQueryServiceInMemory apiQueryServiceInMemory;

    @Autowired
    private ParametersQueryServiceInMemory parametersQueryService;

    @Autowired
    private RoleQueryServiceInMemory roleQueryService;

    @Autowired
    private UserCrudServiceInMemory userCrudService;

    @Autowired
    private CreateApiDomainService createApiDomainService;

    @Autowired
    private ValidateApiDomainService validateApiDomainService;

    @Autowired
    private ValidateResourceDomainServiceInMemory validateResourceDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    void setup() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environment);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        apiQueryServiceInMemory.reset();
        reset(createApiDomainService, validateApiDomainService);
    }

    @Nested
    class VerifyHosts {

        WebTarget verifyHostsTarget;

        @BeforeEach
        void setUp() {
            final Api tcpApi = aTcpApiV4().toBuilder().id("tcp-1").environmentId(ENVIRONMENT).build();
            apiQueryServiceInMemory.initWith(List.of(tcpApi));

            verifyHostsTarget = rootTarget().path("_verify/hosts");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void should_catch_invalid_host_exception(List<String> hosts) {
            // given
            VerifyApiHosts verifyApiHosts = new VerifyApiHosts();
            verifyApiHosts.setApiId("tcp-1");
            verifyApiHosts.setHosts(hosts);

            // when
            final Response response = verifyHostsTarget.request().post(Entity.json(verifyApiHosts));

            // then
            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(VerifyApiHostsResponse.class)
                .isEqualTo(new VerifyApiHostsResponse().ok(false).reason("At least one host is required for the TCP listener."));
        }

        @Test
        void should_catch_duplicated_host_exception() {
            // given
            VerifyApiHosts verifyApiHosts = new VerifyApiHosts();
            verifyApiHosts.setApiId("tcp-1");
            verifyApiHosts.setHosts(List.of("tcp.example.com", "tcp.example.com", "tcp-2.example.com", "tcp-2.example.com"));

            // when
            final Response response = verifyHostsTarget.request().post(Entity.json(verifyApiHosts));

            // then
            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(VerifyApiHostsResponse.class)
                .isEqualTo(
                    new VerifyApiHostsResponse()
                        .ok(false)
                        .reason("Duplicated hosts detected: 'tcp.example.com, tcp-2.example.com'. Please ensure each host is unique.")
                );
        }

        @Test
        void should_catch_host_already_exist_exception() {
            // given
            VerifyApiHosts verifyApiHosts = new VerifyApiHosts();
            verifyApiHosts.setApiId("tcp-2");
            verifyApiHosts.setHosts(List.of("foo.example.com", "tcp.example.com", "bar.example.com"));

            // when
            final Response response = verifyHostsTarget.request().post(Entity.json(verifyApiHosts));

            // then
            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(VerifyApiHostsResponse.class)
                .isEqualTo(new VerifyApiHostsResponse().ok(false).reason("Hosts [foo.example.com, bar.example.com] already exists"));
        }

        @Test
        void should_validate_hosts() {
            // given
            VerifyApiHosts verifyApiHosts = new VerifyApiHosts();
            verifyApiHosts.setApiId("tcp-2");
            verifyApiHosts.setHosts(List.of("tcp-1.example.com", "tcp-2.example.com"));

            // when
            final Response response = verifyHostsTarget.request().post(Entity.json(verifyApiHosts));

            // then
            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(VerifyApiHostsResponse.class)
                .isEqualTo(new VerifyApiHostsResponse().ok(true));
        }
    }

    @Nested
    class CreateApi {

        WebTarget target;

        @BeforeEach
        void setUp() {
            target = rootTarget();

            roleQueryService.resetSystemRoles(ORGANIZATION);
            parametersQueryService.initWith(
                List.of(new Parameter(Key.API_PRIMARY_OWNER_MODE.key(), ENVIRONMENT, ParameterReferenceType.ENVIRONMENT, "USER"))
            );
            userCrudService.initWith(
                List.of(BaseUserEntity.builder().id(USER_NAME).firstname("John").lastname("Doe").email("john.doe@gravitee.io").build())
            );
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_API),
                    eq(ENVIRONMENT),
                    any()
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(new CreateApiV4()));

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_400_when_no_content() {
            final Response response = target.request().post(null);

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_an_api_without_name() {
            final Response response = target
                .request()
                .post(
                    Entity.json(
                        new CreateApiV4()
                            .type(ApiType.PROXY)
                            .listeners(
                                List.of(
                                    new Listener(
                                        (HttpListener) new HttpListener()
                                            .paths(List.of(new PathV4().path("/path")))
                                            .entrypoints(List.of(new Entrypoint().type("sse")))
                                    )
                                )
                            )
                            .apiVersion("v1")
                            .name("")
                    )
                );

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_an_api_without_listeners() {
            final Response response = target
                .request()
                .post(Entity.json(new CreateApiV4().type(ApiType.PROXY).name("no-listeners").apiVersion("v1")));

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_native_api_has_multiple_flows() {
            doThrow(new NativeApiWithMultipleFlowsException()).when(createApiDomainService).create(any(), any(), any(), any(), any());

            var newApi = aValidNativeV4Api().flows(List.of(new FlowV4(), new FlowV4()));

            final Response response = target.request().post(Entity.json(newApi));

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_an_api_without_endpoints() {
            final Response response = target
                .request()
                .post(
                    Entity.json(
                        new CreateApiV4()
                            .listeners(
                                List.of(
                                    new Listener(
                                        (HttpListener) new HttpListener()
                                            .paths(List.of(new PathV4().path("/path")))
                                            .entrypoints(List.of(new Entrypoint().type("sse")))
                                    )
                                )
                            )
                            .type(ApiType.PROXY)
                            .name("no-endpoints")
                            .apiVersion("v1")
                    )
                );

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_created_api() {
            when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(verifyApiPathDomainService.validateAndSanitize(any())).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

            when(createApiDomainService.create(any(Api.class), any(), any(AuditInfo.class), any(), any()))
                .thenAnswer(invocation -> {
                    Api api = invocation.getArgument(0);
                    return new ApiWithFlows(api.toBuilder().id("api-id").build(), api.getApiDefinitionHttpV4().getFlows());
                });

            var newApi = aValidV4Api();

            final Response response = target.request().post(Entity.json(newApi));

            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(ApiV4.class)
                .satisfies(api ->
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(api.getId()).isEqualTo("api-id");
                        soft.assertThat(api.getAnalytics()).isEqualTo(newApi.getAnalytics());
                        soft.assertThat(api.getApiVersion()).isEqualTo(newApi.getApiVersion());
                        soft.assertThat(api.getEndpointGroups()).isEqualTo(newApi.getEndpointGroups());
                        soft.assertThat(api.getDescription()).isEqualTo(newApi.getDescription());
                        soft.assertThat(api.getDefinitionVersion()).isEqualTo(newApi.getDefinitionVersion());
                        soft.assertThat(api.getFlowExecution()).isEqualTo(newApi.getFlowExecution());
                        soft.assertThat(api.getFlows()).isEqualTo(newApi.getFlows());
                        soft.assertThat(api.getGroups()).isEqualTo(newApi.getGroups());
                        soft.assertThat(api.getListeners()).isEqualTo(newApi.getListeners());
                        soft.assertThat(api.getName()).isEqualTo(newApi.getName());
                        soft.assertThat(api.getTags()).containsExactlyElementsOf(newApi.getTags());
                        soft.assertThat(api.getType()).isEqualTo(newApi.getType());
                    })
                );
        }

        @Test
        void should_return_created_native_api() {
            when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(verifyApiPathDomainService.validateAndSanitize(any())).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

            when(createApiDomainService.create(any(Api.class), any(), any(AuditInfo.class), any(), any()))
                .thenAnswer(invocation -> {
                    Api api = invocation.getArgument(0);
                    return new ApiWithFlows(api.toBuilder().id("api-id").build(), api.getApiDefinitionNativeV4().getFlows());
                });

            var newApi = aValidNativeV4Api();

            final Response response = target.request().post(Entity.json(newApi));

            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(ApiV4.class)
                .satisfies(api ->
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(api.getId()).isEqualTo("api-id");
                        soft.assertThat(api.getAnalytics()).isEqualTo(newApi.getAnalytics());
                        soft.assertThat(api.getApiVersion()).isEqualTo(newApi.getApiVersion());
                        soft.assertThat(api.getEndpointGroups()).isEqualTo(newApi.getEndpointGroups());
                        soft.assertThat(api.getDescription()).isEqualTo(newApi.getDescription());
                        soft.assertThat(api.getDefinitionVersion()).isEqualTo(newApi.getDefinitionVersion());
                        soft.assertThat(api.getFlowExecution()).isEqualTo(newApi.getFlowExecution());
                        soft.assertThat(api.getFlows()).isEqualTo(newApi.getFlows());
                        soft.assertThat(api.getGroups()).isEqualTo(newApi.getGroups());
                        soft.assertThat(api.getListeners()).isEqualTo(newApi.getListeners());
                        soft.assertThat(api.getName()).isEqualTo(newApi.getName());
                        soft.assertThat(api.getTags()).containsExactlyElementsOf(newApi.getTags());
                        soft.assertThat(api.getType()).isEqualTo(newApi.getType());
                    })
                );
        }

        @Test
        void should_return_created_native_api_without_kafka_listener_port() {
            when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(verifyApiPathDomainService.validateAndSanitize(any())).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

            when(createApiDomainService.create(any(Api.class), any(), any(AuditInfo.class), any(), any()))
                .thenAnswer(invocation -> {
                    Api api = invocation.getArgument(0);
                    return new ApiWithFlows(api.toBuilder().id("api-id").build(), api.getApiDefinitionNativeV4().getFlows());
                });

            var newApi = aValidNativeV4Api();
            var kafkaListenerWithoutPort = newApi.getListeners().getFirst().getKafkaListener().port(null);
            newApi.setListeners(List.of(new Listener(kafkaListenerWithoutPort)));

            final Response response = target.request().post(Entity.json(newApi));

            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(ApiV4.class)
                .satisfies(api ->
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(api.getId()).isEqualTo("api-id");
                        soft.assertThat(api.getListeners()).isEqualTo(newApi.getListeners());
                    })
                );
        }

        private static CreateApiV4 aValidV4Api() {
            return (CreateApiV4) new CreateApiV4()
                .analytics(new Analytics().enabled(true))
                .type(ApiType.PROXY)
                .tags(Set.of("tag1"))
                .listeners(
                    List.of(
                        new Listener(
                            (HttpListener) new HttpListener()
                                .paths(List.of(new PathV4().path("/path").overrideAccess(false)))
                                .type(ListenerType.HTTP)
                                .entrypoints(List.of(new Entrypoint().type("sse").qos(Qos.AUTO)))
                        )
                    )
                )
                .endpointGroups(
                    List.of(
                        new EndpointGroupV4()
                            .name("default-group")
                            .type("http")
                            .endpoints(
                                List.of(
                                    new EndpointV4()
                                        .name("default")
                                        .type("kafka")
                                        .weight(1)
                                        .secondary(false)
                                        .inheritConfiguration(false)
                                        .configuration(Map.ofEntries(Map.entry("bootstrapServers", "kafka:9092")))
                                        .sharedConfigurationOverride(
                                            Map.ofEntries(
                                                Map.entry(
                                                    "consumer",
                                                    Map.ofEntries(
                                                        Map.entry("enabled", true),
                                                        Map.entry("topics", List.of("demo")),
                                                        Map.entry("autoOffsetReset", "earliest")
                                                    )
                                                )
                                            )
                                        )
                                )
                            )
                    )
                )
                .flowExecution(new FlowExecution().mode(FlowMode.BEST_MATCH).matchRequired(true))
                .flows(
                    List.of(
                        new FlowV4()
                            .name("flowName")
                            .enabled(true)
                            .tags(Set.of("tag1"))
                            .request(List.of(new StepV4().enabled(true).policy("my-policy").condition("my-condition")))
                            .selectors(
                                List.of(
                                    new Selector(
                                        (HttpSelector) new HttpSelector()
                                            .path("/test")
                                            .methods(Set.of(HttpMethod.GET, HttpMethod.POST))
                                            .pathOperator(Operator.STARTS_WITH)
                                            .type(BaseSelector.TypeEnum.HTTP)
                                    )
                                )
                            )
                    )
                )
                .name("my api")
                .description("api description")
                .definitionVersion(DefinitionVersion.V4)
                .groups(List.of("group1"))
                .apiVersion("v1");
        }

        private static CreateApiV4 aValidNativeV4Api() {
            return (CreateApiV4) new CreateApiV4()
                .type(ApiType.NATIVE)
                .tags(Set.of("tag1"))
                .listeners(
                    List.of(
                        new Listener(
                            (KafkaListener) new KafkaListener()
                                .host("host")
                                .port(4000)
                                .type(ListenerType.KAFKA)
                                .entrypoints(List.of(new Entrypoint().type("mock").qos(Qos.AUTO)))
                        )
                    )
                )
                .endpointGroups(
                    List.of(
                        new EndpointGroupV4()
                            .name("default-group")
                            .type("http")
                            .endpoints(
                                List.of(
                                    new EndpointV4()
                                        .name("default")
                                        .type("kafka")
                                        .weight(1)
                                        .secondary(false)
                                        .inheritConfiguration(false)
                                        .configuration(Map.ofEntries(Map.entry("bootstrapServers", "kafka:9092")))
                                        .sharedConfigurationOverride(
                                            Map.ofEntries(
                                                Map.entry(
                                                    "consumer",
                                                    Map.ofEntries(
                                                        Map.entry("enabled", true),
                                                        Map.entry("topics", List.of("demo")),
                                                        Map.entry("autoOffsetReset", "earliest")
                                                    )
                                                )
                                            )
                                        )
                                )
                            )
                    )
                )
                .flows(
                    List.of(
                        new FlowV4()
                            .name("flowName")
                            .enabled(true)
                            .tags(Set.of("tag1"))
                            .connect(List.of(new StepV4().enabled(true).policy("my-policy").condition("my-condition")))
                    )
                )
                .name("my api")
                .description("api description")
                .definitionVersion(DefinitionVersion.V4)
                .groups(List.of("group1"))
                .apiVersion("v1");
        }
    }

    @Nested
    class ImportCRD {

        WebTarget target;

        @BeforeEach
        void setUp() {
            target = rootTarget().path("/_import/crd");
            apiCrudService.reset();
            categoryQueryService.reset();
            categoryQueryService.initWith(
                List.of(Category.builder().id("category-id").build(), Category.builder().id("category-key").build())
            );
            when(verifyApiPathDomainService.validateAndSanitize(any())).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));
            roleQueryService.initWith(
                List.of(
                    Role
                        .builder()
                        .name(PRIMARY_OWNER.name())
                        .referenceType(Role.ReferenceType.ORGANIZATION)
                        .referenceId(ORGANIZATION)
                        .id("primary_owner_id")
                        .scope(Role.Scope.API)
                        .build()
                )
            );
        }

        @Test
        void should_return_category_warning_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/with-unknown-category.json", true);
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(
                        ApiCRDStatus
                            .builder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .crossId("f4feb2f7-ae13-47bc-800f-289592105119")
                            .id(("63cb34e5-e5cb-40cf-94ca-4687e7813473"))
                            .plan("API_KEY", "6bf5ca72-e70b-4f59-b0a6-b5dca782ce24")
                            .state("STARTED")
                            .errors(
                                ApiCRDStatus.Errors
                                    .builder()
                                    .warning(List.of("category [unknown-category] is not defined in environment [fake-env]"))
                                    .severe(List.of())
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(apiCrudService.storage()).isEmpty();
            });
        }

        @Test
        void should_return_member_warning_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/with-unknown-member.json", true);
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(
                        ApiCRDStatus
                            .builder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .crossId("f4feb2f7-ae13-47bc-800f-289592105119")
                            .id(("63cb34e5-e5cb-40cf-94ca-4687e7813473"))
                            .plan("API_KEY", "6bf5ca72-e70b-4f59-b0a6-b5dca782ce24")
                            .state("STARTED")
                            .errors(
                                ApiCRDStatus.Errors
                                    .builder()
                                    .warning(List.of("member [unknown] of source [memory] could not be found in organization [fake-org]"))
                                    .severe(List.of())
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(apiCrudService.storage()).isEmpty();
            });
        }

        @Test
        void should_return_group_warning_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/with-unknown-group.json", true);
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(
                        ApiCRDStatus
                            .builder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .crossId("f4feb2f7-ae13-47bc-800f-289592105119")
                            .id(("63cb34e5-e5cb-40cf-94ca-4687e7813473"))
                            .plan("API_KEY", "6bf5ca72-e70b-4f59-b0a6-b5dca782ce24")
                            .state("STARTED")
                            .errors(
                                ApiCRDStatus.Errors
                                    .builder()
                                    .warning(List.of("Group [unknown-group] could not be found in environment [fake-env]"))
                                    .severe(List.of())
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(apiCrudService.storage()).isEmpty();
            });
        }

        @Test
        void should_return_resource_warning_in_status_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/with-valid-resource.json", true);
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(
                        ApiCRDStatus
                            .builder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .crossId("f4feb2f7-ae13-47bc-800f-289592105119")
                            .id(("63cb34e5-e5cb-40cf-94ca-4687e7813473"))
                            .plan("API_KEY", "6bf5ca72-e70b-4f59-b0a6-b5dca782ce24")
                            .state("STARTED")
                            .build()
                    );

                soft.assertThat(apiCrudService.storage()).isEmpty();
            });
        }

        @Test
        void should_return_fetcher_error_and_warning_without_saving_if_dry_run() {
            var crdStatus = doImport("/crd/with-invalid-github-fetcher.json", true);
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(
                        ApiCRDStatus
                            .builder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .crossId("f4feb2f7-ae13-47bc-800f-289592105119")
                            .id(("63cb34e5-e5cb-40cf-94ca-4687e7813473"))
                            .plan("API_KEY", "6bf5ca72-e70b-4f59-b0a6-b5dca782ce24")
                            .state("STARTED")
                            .errors(
                                ApiCRDStatus.Errors
                                    .builder()
                                    .severe(List.of("property [owner] is required in [github-fetcher] configuration for page [swagger]"))
                                    .warning(List.of())
                                    .build()
                            )
                            .build()
                    );

                soft.assertThat(apiCrudService.storage()).isEmpty();
            });
        }

        @Test
        void should_return_pages_without_error_status_and_no_saving_if_dry_run() {
            var crdStatus = doImport("/crd/with-valid-page-resource-config.json", true);
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(crdStatus)
                    .isEqualTo(
                        ApiCRDStatus
                            .builder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .crossId("f4feb2f7-ae13-47bc-800f-289592105119")
                            .id(("63cb34e5-e5cb-40cf-94ca-4687e7813473"))
                            .plan("API_KEY", "6bf5ca72-e70b-4f59-b0a6-b5dca782ce24")
                            .state("STARTED")
                            .build()
                    );

                soft.assertThat(apiCrudService.storage()).isEmpty();
            });
        }

        private ApiCRDStatus doImport(String crdResource, boolean dryRun) {
            try (var response = target.queryParam("dryRun", dryRun).request().put(Entity.json(readJSON(crdResource)))) {
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
                return response.readEntity(ApiCRDStatus.class);
            }
        }

        private String readJSON(String resource) {
            try (var reader = this.getClass().getResourceAsStream(resource)) {
                return IOUtils.toString(reader, Charset.defaultCharset());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
