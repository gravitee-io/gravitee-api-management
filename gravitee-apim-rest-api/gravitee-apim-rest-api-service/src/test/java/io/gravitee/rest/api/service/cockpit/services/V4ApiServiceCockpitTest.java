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
package io.gravitee.rest.api.service.cockpit.services;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.IndexerInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.NotificationConfigCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.WorkflowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.reactivex.rxjava3.observers.TestObserver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ashraful Hasan (ashraful.hasan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class V4ApiServiceCockpitTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String GROUP_ID = "group-id";

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    NotificationConfigCrudServiceInMemory notificationConfigCrudService = new NotificationConfigCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    IndexerInMemory indexer = new IndexerInMemory();

    @Mock
    ValidateApiDomainService validateApiDomainService;

    @Mock
    private ApiService apiServiceV4;

    @Mock
    private ApiStateService apiStateService;

    private V4ApiServiceCockpitImpl service;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    public void setUp() throws Exception {
        validateApiDomainService = mock(ValidateApiDomainService.class);

        var metadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
        var membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerFactory = new ApiPrimaryOwnerFactory(
            groupQueryService,
            membershipQueryService,
            parametersQueryService,
            roleQueryService,
            userCrudService
        );
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        var createApiDomainService = new CreateApiDomainService(
            apiCrudService,
            auditService,
            new ApiIndexerDomainService(
                new ApiMetadataDecoderDomainService(metadataQueryService, new FreemarkerTemplateProcessor()),
                apiPrimaryOwnerDomainService,
                new ApiCategoryQueryServiceInMemory(),
                indexer
            ),
            new ApiMetadataDomainService(metadataCrudService, apiMetadataQueryService, auditService),
            apiPrimaryOwnerDomainService,
            flowCrudService,
            notificationConfigCrudService,
            parametersQueryService,
            workflowCrudService
        );

        service =
            new V4ApiServiceCockpitImpl(
                apiPrimaryOwnerFactory,
                validateApiDomainService,
                createApiDomainService,
                apiServiceV4,
                apiStateService
            );

        lenient()
            .when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                )
            )
        );
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        givenExistingUsers(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
    }

    @Test
    public void should_create_publish_api() throws JsonProcessingException, InterruptedException {
        when(apiStateService.start(any(), any(String.class), any()))
            .thenAnswer(invocation -> ApiEntity.builder().id(invocation.getArgument(1)).build());
        when(apiStateService.deploy(any(), any(String.class), any(), any()))
            .thenAnswer(invocation -> ApiEntity.builder().id(invocation.getArgument(1)).build());
        when(apiServiceV4.update(any(), any(), any(), any()))
            .thenAnswer(invocation -> ApiEntity.builder().id(invocation.getArgument(1)).build());

        TestObserver<ApiEntity> observer = service
            .createPublishApi(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID, validApiDefinition())
            .test()
            .await()
            .assertValue(Objects::nonNull);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiCrudService.storage()).hasSize(1);
            var created = apiCrudService.storage().get(0);

            soft.assertThat(created.getId()).isEqualTo("generated-id");
            soft.assertThat(created.getName()).isEqualTo("Original");
            soft.assertThat(created.getVersion()).isEqualTo("1.0");
            soft.assertThat(created.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
            soft.assertThat(created.getType()).isEqualTo(ApiType.PROXY);
            soft.assertThat(created.getDescription()).isEqualTo("Original from Cockpit - HTTP");
            // Resources and Properties are currently not handle by NewApi
            soft
                .assertThat(created.getApiDefinitionHttpV4())
                .isEqualTo(
                    Api
                        .builder()
                        .id("generated-id")
                        .apiVersion("1.0")
                        .name("Original")
                        .type(ApiType.PROXY)
                        .tags(Set.of())
                        .analytics(Analytics.builder().enabled(true).build())
                        .listeners(
                            List.of(
                                HttpListener
                                    .builder()
                                    .entrypoints(List.of(Entrypoint.builder().type("http-proxy").qos(Qos.AUTO).build()))
                                    .paths(List.of(Path.builder().path("/original/http-proxy/").build()))
                                    .build()
                            )
                        )
                        .endpointGroups(
                            List.of(
                                EndpointGroup
                                    .builder()
                                    .name("default-group")
                                    .type("http-proxy")
                                    .endpoints(
                                        List.of(
                                            Endpoint
                                                .builder()
                                                .name("default")
                                                .type("http-proxy")
                                                .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                                                .build()
                                        )
                                    )
                                    .build()
                            )
                        )
                        .flowExecution(new FlowExecution())
                        .flows(List.of())
                        .build()
                );
        });
    }

    @Test
    public void should_start_and_deploy_api() throws JsonProcessingException, InterruptedException {
        when(apiStateService.start(any(), any(String.class), any()))
            .thenAnswer(invocation -> ApiEntity.builder().id(invocation.getArgument(1)).build());
        when(apiStateService.deploy(any(), any(String.class), any(), any()))
            .thenAnswer(invocation -> ApiEntity.builder().id(invocation.getArgument(1)).build());
        when(apiServiceV4.update(any(), any(), any(), any()))
            .thenAnswer(invocation -> ApiEntity.builder().id(invocation.getArgument(1)).build());

        TestObserver<ApiEntity> observer = service.createPublishApi(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID, validApiDefinition()).test();
        observer.await();

        observer.assertValue(Objects::nonNull);
        verify(apiServiceV4, times(1)).update(any(), any(), any(), any());
        verify(apiStateService, times(1)).start(any(), any(), any());
        verify(apiStateService, times(1)).deploy(any(), any(String.class), any(), any());
    }

    @Test
    public void should_throw_exception() {
        final String userId = "any-user-id";
        assertThrows(
            JsonProcessingException.class,
            () -> service.createPublishApi("organization-id", "environment-id", userId, "{invalid-json}")
        );
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
    }

    private String validApiDefinition() {
        return (
            """
                {
                   "newApiEntity":{
                      "name":"Original",
                      "apiVersion":"1.0",
                      "definitionVersion":"4.0.0",
                      "type":"proxy",
                      "description":"Original from Cockpit - HTTP",
                      "tags":[
                        \s
                      ],
                      "listeners":[
                         {
                            "type":"http",
                            "entrypoints":[
                               {
                                  "type":"http-proxy",
                                  "qos":"auto"
                               }
                            ],
                            "paths":[
                               {
                                  "path":"/original/http-proxy/"
                               }
                            ]
                         }
                      ],
                      "endpointGroups":[
                         {
                            "name":"default-group",
                            "type":"http-proxy",
                            "loadBalancer":{
                              \s
                            },
                            "endpoints":[
                               {
                                  "name":"default",
                                  "type":"http-proxy",
                                  "secondary":false,
                                  "weight":1,
                                  "inheritConfiguration":false,
                                  "configuration":{
                                     "target":"https://api.gravitee.io/echo"
                                  },
                                  "services":{
                                    \s
                                  }
                               }
                            ],
                            "services":{
                              \s
                            }
                         }
                      ],
                      "analytics":{
                         "enabled":true
                      },
                      "flowExecution":{
                         "mode":"default",
                         "matchRequired":false
                      },
                      "flows":[
                        \s
                      ],
                      "resources":[
                         {
                            "name":"my-cache",
                            "type":"cache",
                            "configuration":{
                               "maxEntriesLocalHeap":1000,
                               "timeToIdleSeconds":2,
                               "timeToLiveSeconds":4
                            },
                            "enabled":true
                         },
                         {
                            "name":"oauth",
                            "type":"oauth2",
                            "configuration":{
                               "authorizationServerUrl":"https://authorization_server",
                               "introspectionEndpoint":"/oauth/check_token",
                               "useSystemProxy":false,
                               "introspectionEndpointMethod":"GET",
                               "scopeSeparator":" ",
                               "userInfoEndpoint":"/userinfo",
                               "userInfoEndpointMethod":"GET",
                               "useClientAuthorizationHeader":true,
                               "clientAuthorizationHeaderName":"Authorization",
                               "clientAuthorizationHeaderScheme":"Basic",
                               "tokenIsSuppliedByQueryParam":true,
                               "tokenQueryParamName":"token",
                               "tokenIsSuppliedByHttpHeader":false,
                               "tokenIsSuppliedByFormUrlEncoded":false,
                               "tokenFormUrlEncodedName":"token",
                               "userClaim":"sub",
                               "clientId":"client-id",
                               "clientSecret":"client-secret"
                            },
                            "enabled":true
                         }
                      ],
                      "properties":[
                         {
                            "key":"client-id",
                            "value":"abc",
                            "encrypted":false,
                            "dynamic":false,
                            "encryptable":false
                         },
                         {
                            "key":"client-secret",
                            "value":"abc",
                            "encrypted":false,
                            "dynamic":false,
                            "encryptable":false
                         },
                         {
                            "key":"property-1",
                            "value":"value-1",
                            "encrypted":false,
                            "dynamic":false,
                            "encryptable":false
                         },
                         {
                            "key":"property-2",
                            "value":"EnqXzj3i27jDUZU8h6fIqg==",
                            "encrypted":true,
                            "dynamic":false,
                            "encryptable":false
                         }
                      ]
                   },
                   "planEntities":[
                      {
                         "name":"Keyless",
                         "description":"Keyless",
                         "createdAt":1689169972399,
                         "updatedAt":1689169972399,
                         "publishedAt":1689169972399,
                         "validation":"auto",
                         "type":"api",
                         "mode":"standard",
                         "security":{
                            "type":"KEY_LESS"
                         },
                         "flows":[
                           \s
                         ],
                         "tags":[
                           \s
                         ],
                         "status":"published",
                         "order":1,
                         "commentRequired":false
                      },
                      {
                         "name":"Premium API Key Plan",
                         "description":"",
                         "createdAt":1689169972379,
                         "updatedAt":1689170527231,
                         "publishedAt":1689169972379,
                         "validation":"auto",
                         "type":"api",
                         "mode":"standard",
                         "security":{
                            "type":"API_KEY",
                            "configuration":{
                              \s
                            }
                         },
                         "flows":[
                            {
                              \s
                            }
                         ],
                         "tags":[
                           \s
                         ],
                         "status":"published",
                         "order":1,
                         "characteristics":[
                           \s
                         ],
                         "excludedGroups":[
                           \s
                         ],
                         "commentRequired":false,
                         "commentMessage":"",
                         "generalConditions":""
                      },
                      {
                         "name":"Keyless",
                         "description":"Keyless",
                         "createdAt":1689169964283,
                         "updatedAt":1689171419581,
                         "publishedAt":1689169964307,
                         "closedAt":1689171419581,
                         "validation":"auto",
                         "type":"api",
                         "mode":"standard",
                         "security":{
                            "type":"KEY_LESS"
                         },
                         "flows":[
                           \s
                         ],
                         "tags":[
                           \s
                         ],
                         "status":"closed",
                         "order":1,
                         "commentRequired":false
                      },
                      {
                         "name":"Limit Creation of Tasks",
                         "description":"",
                         "createdAt":1689169972429,
                         "updatedAt":1689169972429,
                         "validation":"manual",
                         "type":"api",
                         "mode":"standard",
                         "security":{
                            "type":"API_KEY",
                            "configuration":{
                               "propagateApiKey":false
                            }
                         },
                         "flows":[
                            {
                              \s
                            }
                         ],
                         "tags":[
                           \s
                         ],
                         "status":"closed",
                         "order":3,
                         "characteristics":[
                           \s
                         ],
                         "excludedGroups":[
                           \s
                         ],
                         "commentRequired":false,
                         "commentMessage":"",
                         "generalConditions":""
                      },
                      {
                         "name":"Limit Creation of Tasks",
                         "description":"",
                         "createdAt":1689169972411,
                         "updatedAt":1689169972411,
                         "publishedAt":1689169972411,
                         "validation":"manual",
                         "type":"api",
                         "mode":"standard",
                         "security":{
                            "type":"API_KEY",
                            "configuration":{
                              \s
                            }
                         },
                         "flows":[
                            {
                              \s
                            }
                         ],
                         "tags":[
                           \s
                         ],
                         "status":"published",
                         "order":2,
                         "characteristics":[
                           \s
                         ],
                         "excludedGroups":[
                           \s
                         ],
                         "commentRequired":false,
                         "commentMessage":"",
                         "generalConditions":""
                      }
                   ],
                   "metadata":[
                      {
                         "key":"test-2",
                         "name":"test-2",
                         "format":"STRING",
                         "defaultValue":"value 2"
                      },
                      {
                         "key":"email-support",
                         "name":"email-support",
                         "format":"MAIL",
                         "value":"${(api.primaryOwner.email)!''}",
                         "defaultValue":"support@change.me"
                      },
                      {
                         "key":"test-1",
                         "name":"test 1",
                         "format":"STRING",
                         "defaultValue":"value 1"
                      }
                   ]
                }"""
        );
    }
}
