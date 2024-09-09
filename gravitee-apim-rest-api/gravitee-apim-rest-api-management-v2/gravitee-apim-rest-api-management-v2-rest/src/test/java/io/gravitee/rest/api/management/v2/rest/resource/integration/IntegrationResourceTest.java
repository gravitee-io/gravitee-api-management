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
package io.gravitee.rest.api.management.v2.rest.resource.integration;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.rest.api.management.v2.rest.model.IngestionPreviewResponseApisInner.StateEnum.NEW;
import static io.gravitee.rest.api.management.v2.rest.resource.integration.IntegrationsResourceTest.INTEGRATION_PROVIDER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AsyncJobFixture;
import fixtures.core.model.IntegrationApiFixtures;
import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AsyncJobCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationAgentInMemory;
import inmemory.IntegrationCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.ApisIngest;
import io.gravitee.rest.api.management.v2.rest.model.DeletedIngestedApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.IngestedApi;
import io.gravitee.rest.api.management.v2.rest.model.IngestedApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.IngestionPreviewResponse;
import io.gravitee.rest.api.management.v2.rest.model.IngestionPreviewResponseApisInner;
import io.gravitee.rest.api.management.v2.rest.model.IngestionStatus;
import io.gravitee.rest.api.management.v2.rest.model.Integration;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationIngestionResponse;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.PrimaryOwner;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

public class IntegrationResourceTest extends AbstractResourceTest {

    @Autowired
    IntegrationCrudServiceInMemory integrationCrudServiceInMemory;

    @Autowired
    AsyncJobCrudServiceInMemory asyncJobCrudServiceInMemory;

    @Autowired
    ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Autowired
    IntegrationAgentInMemory integrationAgentInMemory;

    @Autowired
    LicenseManager licenseManager;

    static final String ENVIRONMENT = "my-env";
    static final String INTEGRATION_ID = "integration-id";

    WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/integrations/" + INTEGRATION_ID;
    }

    @BeforeEach
    public void init() throws TechnicalException {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        roleQueryService.resetSystemRoles(ORGANIZATION);
        enableApiPrimaryOwnerMode(ENVIRONMENT, ApiPrimaryOwnerMode.USER);
        givenExistingUsers(
            List.of(BaseUserEntity.builder().id(USER_NAME).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        Stream
            .of(
                apiCrudServiceInMemory,
                integrationCrudServiceInMemory,
                asyncJobCrudServiceInMemory,
                membershipQueryServiceInMemory,
                integrationAgentInMemory
            )
            .forEach(InMemoryAlternative::reset);
        GraviteeContext.cleanContext();
        reset(licenseManager);
    }

    @Nested
    class GetIntegration {

        @BeforeEach
        void setup() {
            membershipQueryServiceInMemory.initWith(
                List.of(
                    Membership
                        .builder()
                        .memberId(USER_NAME)
                        .referenceId(INTEGRATION_ID)
                        .roleId("int-po-id-fake-org")
                        .referenceType(Membership.ReferenceType.INTEGRATION)
                        .memberType(Membership.Type.USER)
                        .build()
                )
            );
        }

        @Test
        public void should_get_integration() {
            // Given
            var integration = givenAnIntegration(IntegrationFixture.anIntegration().withId(INTEGRATION_ID));

            //When
            Response response = target.request().get();

            //Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(Integration.class)
                .isEqualTo(
                    Integration
                        .builder()
                        .id(INTEGRATION_ID)
                        .name(integration.getName())
                        .description(integration.getDescription())
                        .provider(integration.getProvider())
                        .agentStatus(Integration.AgentStatusEnum.CONNECTED)
                        .primaryOwner(PrimaryOwner.builder().id("UnitTests").email("jane.doe@gravitee.io").displayName("Jane Doe").build())
                        .groups(List.of())
                        .build()
                );
        }

        @Test
        public void should_get_integration_with_pending_job() {
            // Given
            var integration = givenAnIntegration(IntegrationFixture.anIntegration().withId(INTEGRATION_ID));
            var job = givenAnAsyncJob(AsyncJobFixture.aPendingFederatedApiIngestionJob().withSourceId(INTEGRATION_ID));

            //When
            Response response = target.request().get();

            //Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(Integration.class)
                .isEqualTo(
                    Integration
                        .builder()
                        .id(INTEGRATION_ID)
                        .name(integration.getName())
                        .description(integration.getDescription())
                        .provider(integration.getProvider())
                        .agentStatus(Integration.AgentStatusEnum.CONNECTED)
                        .primaryOwner(PrimaryOwner.builder().id("UnitTests").email("jane.doe@gravitee.io").displayName("Jane Doe").build())
                        .pendingJob(
                            io.gravitee.rest.api.management.v2.rest.model.IngestionJob
                                .builder()
                                .id(job.getId())
                                .status(IngestionStatus.PENDING)
                                .build()
                        )
                        .groups(List.of())
                        .build()
                );
        }

        @Test
        public void should_throw_error_when_integration_not_found() {
            //Given
            //When
            Response response = target.request().get();

            //Then
            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        public void should_return_403_when_incorrect_permission() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.INTEGRATION_DEFINITION),
                    eq(INTEGRATION_ID),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            Response response = target.request().get();
            assertThat(response).hasStatus(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class IngestApis {

        @BeforeEach
        void setUp() {
            target = rootTarget().path("_ingest");
        }

        @Test
        public void should_get_error_if_user_does_not_have_correct_permissions() {
            //Given
            var entity = Entity.entity(new ApisIngest(), MediaType.APPLICATION_JSON_TYPE);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.INTEGRATION_DEFINITION,
                    INTEGRATION_ID,
                    RolePermissionAction.CREATE
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(entity);

            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        public void should_throw_error_when_integration_not_found() {
            //Given
            var entity = Entity.entity(new ApisIngest(), MediaType.APPLICATION_JSON_TYPE);

            //When
            Response response = target.request().post(entity);

            //Then
            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        public void should_return_success_when_ingestion_has_started() {
            //Given
            var entity = Entity.entity(new ApisIngest().apiIds(List.of()), MediaType.APPLICATION_JSON_TYPE);
            integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration().withId(INTEGRATION_ID)));
            integrationAgentInMemory.configureApisNumberToIngest(INTEGRATION_ID, 10L);

            //When
            Response response = target.request().post(entity);

            //Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(IntegrationIngestionResponse.class)
                .isEqualTo(IntegrationIngestionResponse.builder().status(IngestionStatus.PENDING).build());
        }
    }

    @Nested
    class UpdateIntegration {

        @Test
        public void should_update_integration() {
            //Given
            var updatedName = "updated-name";
            var updatedDescription = "updated-description";
            var integration = List.of(IntegrationFixture.anIntegration());
            integrationCrudServiceInMemory.initWith(integration);

            var updateIntegration = io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration
                .builder()
                .name(updatedName)
                .description(updatedDescription)
                .build();

            //When
            Response response = target.request().put(Entity.json(updateIntegration));

            //Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(Integration.class)
                .isEqualTo(
                    Integration
                        .builder()
                        .id(INTEGRATION_ID)
                        .name(updatedName)
                        .description(updatedDescription)
                        .provider(INTEGRATION_PROVIDER)
                        .build()
                );
        }

        @Test
        public void should_update_integration_with_group() {
            //Given
            var groupId = "group-id";
            var updatedName = "updated-name";
            var updatedDescription = "updated-description";
            var integration = List.of(IntegrationFixture.anIntegration());
            integrationCrudServiceInMemory.initWith(integration);
            givenExistingGroup(List.of(Group.builder().id(groupId).name("group-name").build()));

            var updateIntegration = io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration
                .builder()
                .name(updatedName)
                .description(updatedDescription)
                .groups(List.of(groupId))
                .build();

            //When
            Response response = target.request().put(Entity.json(updateIntegration));

            //Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(Integration.class)
                .isEqualTo(
                    Integration
                        .builder()
                        .id(INTEGRATION_ID)
                        .name(updatedName)
                        .description(updatedDescription)
                        .provider(INTEGRATION_PROVIDER)
                        .groups(List.of(groupId))
                        .build()
                );
        }

        @Test
        public void should_throw_error_when_integration_to_update_not_found() {
            //Given
            var updatedName = "updated-name";
            var updatedDescription = "updated-description";

            var updateIntegration = io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration
                .builder()
                .name(updatedName)
                .description(updatedDescription)
                .build();

            //When
            Response response = target.request().put(Entity.json(updateIntegration));

            //Then
            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        public void should_return_403_when_incorrect_permission() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.INTEGRATION_DEFINITION),
                    eq(INTEGRATION_ID),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            var updateIntegration = io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration.builder().build();

            Response response = target.request().put(Entity.json(updateIntegration));

            assertThat(response).hasStatus(HttpStatusCode.FORBIDDEN_403);
        }

        @Test
        public void should_return_400_when_missing_name_to_update() {
            var updatedDescription = "updated-description";

            var updateIntegration = io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration
                .builder()
                .description(updatedDescription)
                .build();

            Response response = target.request().put(Entity.json(updateIntegration));

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        public void should_return_400_when_missing_body() {
            var updateIntegration = io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration.builder().build();

            Response response = target.request().put(Entity.json(updateIntegration));

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
        }
    }

    @Nested
    class DeleteIntegration {

        @Test
        public void should_delete_integration() {
            //Given
            integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration()));
            //When
            Response response = target.request().delete();

            //Then
            assertThat(response).hasStatus(HttpStatusCode.NO_CONTENT_204);
            assertThat(integrationCrudServiceInMemory.storage().size()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_integration_to_delete_not_found() {
            Response response = target.request().delete();

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        public void should_return_400_when_associated_federated_api_found() {
            integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration()));
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi()));

            Response response = target.request().delete();
            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        public void should_return_403_when_incorrect_permission() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.INTEGRATION_DEFINITION),
                    eq(INTEGRATION_ID),
                    eq(RolePermissionAction.DELETE)
                )
            )
                .thenReturn(false);

            Response response = target.request().delete();

            assertThat(response).hasStatus(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class GetIngestedApis {

        @BeforeEach
        void setUp() {
            var federatedApis = IntStream.range(0, 15).mapToObj(i -> ApiFixtures.aFederatedApi()).toList();
            apiCrudServiceInMemory.initWith(federatedApis);
        }

        @Test
        public void should_return_list_of_ingested_apis_with_default_pagination() {
            Response response = target.path("/apis").request().get();

            assertThat(response)
                .hasStatus(200)
                .asEntity(IngestedApisResponse.class)
                .extracting(IngestedApisResponse::getPagination)
                .isEqualTo(Pagination.builder().page(1).perPage(10).pageItemsCount(10).pageCount(2).totalCount(15L).build());
        }

        @Test
        public void should_return_second_page_of_ingested_apis() {
            Response response = target.path("/apis").queryParam("page", 2).queryParam("perPage", 10).request().get();

            assertThat(response)
                .hasStatus(200)
                .asEntity(IngestedApisResponse.class)
                .extracting(IngestedApisResponse::getPagination)
                .isEqualTo(Pagination.builder().page(2).perPage(10).pageItemsCount(10).pageCount(2).totalCount(15L).build());
        }

        @Test
        public void should_return_sorted_pages_of_ingested_apis() {
            var recentlyUpdatedApi = ApiFixtures
                .aFederatedApi()
                .toBuilder()
                .updatedAt(ZonedDateTime.parse("2024-02-01T20:22:02.00Z"))
                .name("recently-updated")
                .build();
            apiCrudServiceInMemory.create(recentlyUpdatedApi);

            Response response = target.path("/apis").request().get();

            assertThat(response)
                .hasStatus(200)
                .asEntity(IngestedApisResponse.class)
                .extracting(IngestedApisResponse::getData)
                .extracting(ingestedApis -> ingestedApis.get(0))
                .extracting(IngestedApi::getName)
                .isEqualTo(recentlyUpdatedApi.getName());
        }

        @Test
        void should_compute_links() {
            var webtarget = target.path("/apis");
            Response response = webtarget.queryParam("page", 2).queryParam("perPage", 5).request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(IngestedApisResponse.class)
                .extracting(IngestedApisResponse::getLinks)
                .isEqualTo(
                    Links
                        .builder()
                        .self(webtarget.queryParam("page", 2).queryParam("perPage", 5).getUri().toString())
                        .first(webtarget.queryParam("page", 1).queryParam("perPage", 5).getUri().toString())
                        .last(webtarget.queryParam("page", 3).queryParam("perPage", 5).getUri().toString())
                        .previous(webtarget.queryParam("page", 1).queryParam("perPage", 5).getUri().toString())
                        .next(webtarget.queryParam("page", 3).queryParam("perPage", 5).getUri().toString())
                        .build()
                );
        }

        @Test
        public void should_return_403_when_incorrect_permission() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.INTEGRATION_DEFINITION),
                    eq(INTEGRATION_ID),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            Response response = target.path("/apis").request().get();

            assertThat(response).hasStatus(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class DeleteIngestedApis {

        @ParameterizedTest
        @EnumSource(value = Api.ApiLifecycleState.class, mode = EnumSource.Mode.EXCLUDE, names = { "PUBLISHED" })
        public void should_return_deleted_apis(Api.ApiLifecycleState apiLifecycleState) {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(apiLifecycleState).build()));

            Response response = target.path("/apis").request().delete();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(DeletedIngestedApisResponse.class)
                .isEqualTo(new DeletedIngestedApisResponse().deleted(1).skipped(0).errors(0));
        }

        @Test
        public void should_return_skipped_apis() {
            apiCrudServiceInMemory.initWith(
                List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build())
            );

            Response response = target.path("/apis").request().delete();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(DeletedIngestedApisResponse.class)
                .isEqualTo(new DeletedIngestedApisResponse().deleted(0).skipped(1).errors(0));
        }

        @Test
        public void should_return_errors() {
            apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(null).build()));

            Response response = target.path("/apis").request().delete();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(DeletedIngestedApisResponse.class)
                .isEqualTo(new DeletedIngestedApisResponse().deleted(0).skipped(0).errors(1));
        }

        @ParameterizedTest(name = "[{index}] {arguments}")
        @CsvSource(
            delimiterString = "|",
            useHeadersInDisplayName = true,
            textBlock = """
        INTEGRATION_DEFINITION[DELETE] |  ENVIRONMENT_INTEGRATION[DELETE]
        false                  |  false
        true                   |  false
        false                  |  true
     """
        )
        public void should_get_error_if_user_does_not_have_correct_permissions(
            boolean integrationDefinitionDelete,
            boolean environmentIntegrationDelete
        ) {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.INTEGRATION_DEFINITION,
                    ENVIRONMENT,
                    RolePermissionAction.DELETE
                )
            )
                .thenReturn(integrationDefinitionDelete);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_INTEGRATION,
                    ENVIRONMENT,
                    RolePermissionAction.DELETE
                )
            )
                .thenReturn(environmentIntegrationDelete);

            final Response response = target.path("/apis").request().delete();

            assertThat(response).hasStatus(OK_200);
        }
    }

    @Nested
    class PreviewNewFederatedApis {

        @BeforeEach
        void setUp() {
            target = rootTarget().path("_preview");
        }

        @Test
        public void should_get_error_if_user_does_not_have_correct_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.INTEGRATION_DEFINITION,
                    INTEGRATION_ID,
                    RolePermissionAction.CREATE
                )
            )
                .thenReturn(false);

            final Response response = target.request().get();

            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        public void should_return_new_integration_apis_count() {
            //Given
            integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration().withId(INTEGRATION_ID)));
            integrationAgentInMemory.initWith(List.of(IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID)));

            //When
            Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(IngestionPreviewResponse.class)
                .isEqualTo(
                    IngestionPreviewResponse
                        .builder()
                        .totalCount(1)
                        .newCount(1)
                        .updateCount(0)
                        .apis(List.of(IngestionPreviewResponseApisInner.builder().id("asset-id").name("An alien API").state(NEW).build()))
                        .build()
                );
        }
    }

    @Nested
    class GetPermissions {
        private static final TypeReference<Map<String, char[]>> apiType = new TypeReference<>() {};

        @BeforeEach
        void setUp() {
            target = rootTarget().path("permissions");
        }

        @Test
        void asAdmin() {
            //Given
            integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration().withId(INTEGRATION_ID)));
            integrationAgentInMemory.initWith(List.of(IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID)));

            //When
            Response response = target.request().get();

            assertThat(response).hasStatus(OK_200);
            Map<String, char[]> entity = response.readEntity(new GenericType<>() {});
            assertThat(entity)
                    .containsEntry("DEFINITION", new char[]{'C', 'R', 'U', 'D'})
                    .containsEntry("MEMBER", new char[]{'C', 'R', 'U', 'D'});

        }
    }

    private io.gravitee.apim.core.integration.model.Integration givenAnIntegration(
        io.gravitee.apim.core.integration.model.Integration integration
    ) {
        integrationCrudServiceInMemory.initWith(List.of(integration));
        return integration;
    }

    private AsyncJob givenAnAsyncJob(AsyncJob asyncJob) {
        asyncJobCrudServiceInMemory.initWith(List.of(asyncJob));
        return asyncJob;
    }
}
