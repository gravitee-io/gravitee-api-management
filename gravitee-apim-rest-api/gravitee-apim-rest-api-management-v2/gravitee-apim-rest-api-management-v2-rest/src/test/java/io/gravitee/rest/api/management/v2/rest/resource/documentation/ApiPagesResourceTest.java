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
package io.gravitee.rest.api.management.v2.rest.resource.documentation;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.core.model.PlanFixtures;
import inmemory.*;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class ApiPagesResourceTest extends AbstractResourceTest {

    @Autowired
    private ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Autowired
    private PageQueryServiceInMemory pageQueryServiceInMemory;

    @Autowired
    private PageCrudServiceInMemory pageCrudServiceInMemory;

    @Autowired
    private PageRevisionCrudServiceInMemory pageRevisionCrudServiceInMemory;

    @Autowired
    private PlanQueryServiceInMemory planQueryServiceInMemory;

    @Autowired
    private AuditCrudServiceInMemory auditCrudService;

    @Autowired
    private UserCrudServiceInMemory userCrudService;

    @Autowired
    private RoleQueryServiceInMemory roleQueryService;

    @Autowired
    private MembershipQueryServiceInMemory membershipQueryService;

    @Autowired
    private GroupQueryServiceInMemory groupQueryServiceInMemory;

    @Autowired
    private IndexerInMemory indexer;

    protected static final String ENVIRONMENT = "my-env";
    protected static final String API_ID = "api-id";
    protected static final String PAGE_ID = "page-id";
    private static final String ROLE_ID = "role-id";
    private static final String GROUP_ID = "group-id";
    private static final Set<io.gravitee.apim.core.documentation.model.AccessControl> REPO_ACCESS_CONTROLS = Set.of(
        io.gravitee.apim.core.documentation.model.AccessControl.builder().referenceId(ROLE_ID).referenceType("ROLE").build(),
        io.gravitee.apim.core.documentation.model.AccessControl.builder().referenceId(GROUP_ID).referenceType("GROUP").build()
    );

    private static final List<AccessControl> MAPI_V2_ACCESS_ROLES = List.of(
        AccessControl.builder().referenceId(ROLE_ID).referenceType("ROLE").build(),
        AccessControl.builder().referenceId(GROUP_ID).referenceType("GROUP").build()
    );

    @BeforeEach
    void init() {
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        roleQueryService.initWith(
            List.of(
                io.gravitee.apim.core.membership.model.Role
                    .builder()
                    .id(ROLE_ID)
                    .scope(io.gravitee.apim.core.membership.model.Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
        groupQueryServiceInMemory.initWith(List.of(Group.builder().id(GROUP_ID).build()));
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Stream
            .of(
                pageQueryServiceInMemory,
                pageCrudServiceInMemory,
                pageRevisionCrudServiceInMemory,
                auditCrudService,
                apiCrudServiceInMemory,
                planQueryServiceInMemory,
                membershipQueryService,
                groupQueryServiceInMemory,
                roleQueryService,
                indexer
            )
            .forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/api-id/pages";
    }

    @Nested
    class GetApiPagesTest {

        @BeforeEach
        void setUp() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id("api-id").build()));
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_DOCUMENTATION),
                    eq("api-id"),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_get_pages_empty_list() {
            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getPages()).isEqualTo(List.of());
            assertThat(body.getBreadcrumb()).isNull();
        }

        @Test
        void should_get_all_pages_if_no_parameters_specified() {
            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("page-1")
                .name("page-1")
                .excludedAccessControls(true)
                .accessControls(REPO_ACCESS_CONTROLS)
                .build();
            Page page2 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.SWAGGER)
                .id("swagger")
                .name("swagger")
                .build();
            Page page3 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.ASYNCAPI)
                .id("async-api")
                .name("async-api")
                .build();
            Page page4 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("folder")
                .name("folder")
                .build();
            givenApiPagesQuery(List.of(page1, page2, page3, page4));
            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getBreadcrumb()).isNull();
            assertThat(body.getPages())
                .hasSize(4)
                .containsAll(
                    List.of(
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("swagger")
                            .type(PageType.SWAGGER)
                            .name("swagger")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("async-api")
                            .type(PageType.ASYNCAPI)
                            .name("async-api")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("folder")
                            .type(PageType.FOLDER)
                            .name("folder")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .hidden(true)
                            .build()
                    )
                )
                .satisfies(list -> {
                    assertThat(list.get(0))
                        .hasFieldOrPropertyWithValue("id", "page-1")
                        .hasFieldOrPropertyWithValue("type", PageType.MARKDOWN)
                        .hasFieldOrPropertyWithValue("name", "page-1")
                        .hasFieldOrPropertyWithValue("order", 0)
                        .hasFieldOrPropertyWithValue("homepage", false)
                        .hasFieldOrPropertyWithValue("configuration", Map.of())
                        .hasFieldOrPropertyWithValue("metadata", Map.of())
                        .hasFieldOrPropertyWithValue("excludedAccessControls", true)
                        .satisfies(page -> {
                            assertThat(page.getAccessControls())
                                .hasSize(2)
                                .containsAll(
                                    List.of(
                                        AccessControl.builder().referenceId(ROLE_ID).referenceType("ROLE").build(),
                                        AccessControl.builder().referenceId(GROUP_ID).referenceType("GROUP").build()
                                    )
                                );
                        });
                });
        }

        @Test
        void should_get_all_pages_if_empty_parent_id() {
            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("page-1")
                .name("page-1")
                .build();
            Page page2 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.SWAGGER)
                .id("swagger")
                .name("swagger")
                .build();
            Page page3 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.ASYNCAPI)
                .id("async-api")
                .name("async-api")
                .build();
            Page page4 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("folder")
                .name("folder")
                .build();
            givenApiPagesQuery(List.of(page1, page2, page3, page4));
            final Response response = rootTarget().queryParam("parentId", "").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getBreadcrumb()).isNull();
            assertThat(body.getPages())
                .isEqualTo(
                    List.of(
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("page-1")
                            .type(PageType.MARKDOWN)
                            .name("page-1")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("swagger")
                            .type(PageType.SWAGGER)
                            .name("swagger")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("async-api")
                            .type(PageType.ASYNCAPI)
                            .name("async-api")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("folder")
                            .type(PageType.FOLDER)
                            .name("folder")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .hidden(true)
                            .build()
                    )
                );
        }

        @Test
        void should_return_root_pages() {
            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("page-1")
                .name("page-1")
                .parentId("")
                .build();
            Page page2 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.SWAGGER)
                .id("swagger")
                .name("swagger")
                .parentId("")
                .build();
            Page page3 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.ASYNCAPI)
                .id("async-api")
                .name("async-api")
                .parentId("")
                .build();
            Page page4 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("folder-1")
                .name("folder 1")
                .build();
            Page page5 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("folder-2")
                .name("folder 2")
                .parentId("not-root")
                .build();
            givenApiPagesQuery(List.of(page1, page2, page3, page4, page5));
            final Response response = rootTarget().queryParam("parentId", "ROOT").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getBreadcrumb()).isNotNull().hasSize(0);
            assertThat(body.getPages())
                .isEqualTo(
                    List.of(
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("page-1")
                            .type(PageType.MARKDOWN)
                            .name("page-1")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId("")
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("swagger")
                            .type(PageType.SWAGGER)
                            .name("swagger")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId("")
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("async-api")
                            .type(PageType.ASYNCAPI)
                            .name("async-api")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId("")
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("folder-1")
                            .type(PageType.FOLDER)
                            .name("folder 1")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .hidden(true)
                            .build()
                    )
                );
        }

        @Test
        void should_return_pages_of_parent_id() {
            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("page-1")
                .name("page-1")
                .parentId("parent-id")
                .build();
            Page page2 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.SWAGGER)
                .id("swagger")
                .name("swagger")
                .parentId("parent-id")
                .build();
            Page page3 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("folder-1")
                .name("folder 1")
                .build();
            Page page4 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("parent-id")
                .name("folder 2")
                .parentId("")
                .build();
            givenApiPagesQuery(List.of(page1, page2, page3, page4));
            final Response response = rootTarget().queryParam("parentId", "parent-id").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getBreadcrumb())
                .isNotNull()
                .hasSize(1)
                .usingRecursiveComparison()
                .isEqualTo(List.of(Breadcrumb.builder().id("parent-id").name("folder 2").position(1).build()));
            assertThat(body.getPages().get(0))
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.rest.api.management.v2.rest.model.Page
                        .builder()
                        .id("page-1")
                        .type(PageType.MARKDOWN)
                        .name("page-1")
                        .order(0)
                        .published(false)
                        .homepage(false)
                        .configuration(Map.of())
                        .metadata(Map.of())
                        .excludedAccessControls(false)
                        .parentId("parent-id")
                        .generalConditions(false)
                        .build()
                );
            assertThat(body.getPages().get(1))
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.rest.api.management.v2.rest.model.Page
                        .builder()
                        .id("swagger")
                        .type(PageType.SWAGGER)
                        .name("swagger")
                        .order(0)
                        .published(false)
                        .homepage(false)
                        .configuration(Map.of())
                        .metadata(Map.of())
                        .excludedAccessControls(false)
                        .parentId("parent-id")
                        .generalConditions(false)
                        .build()
                );
        }

        @Test
        void should_return_folder_with_published_children() {
            Page child1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("page-1")
                .name("page-1")
                .parentId("parent-id")
                .published(true)
                .build();
            Page child2 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.SWAGGER)
                .id("swagger-1")
                .name("swagger-1")
                .parentId("parent-id")
                .published(true)
                .build();
            Page page = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("parent-id")
                .name("folder 2")
                .parentId("")
                .build();
            givenApiPagesQuery(List.of(child1, child2, page));
            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getPages())
                .usingRecursiveComparison()
                .isEqualTo(
                    List.of(
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("page-1")
                            .type(PageType.MARKDOWN)
                            .name("page-1")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId("parent-id")
                            .published(true)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("swagger-1")
                            .type(PageType.SWAGGER)
                            .name("swagger-1")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId("parent-id")
                            .published(true)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id(page.getId())
                            .type(PageType.FOLDER)
                            .name(page.getName())
                            .order(page.getOrder())
                            .published(page.isPublished())
                            .homepage(page.isHomepage())
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId(page.getParentId())
                            .hidden(false)
                            .build()
                    )
                );
        }

        @Test
        void should_return_swagger_with_published_children() {
            Page child = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.SWAGGER)
                .id("swagger")
                .name("swagger")
                .parentId("parent-id")
                .published(true)
                .build();
            Page page = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("parent-id")
                .name("folder 2")
                .parentId("")
                .build();
            givenApiPagesQuery(List.of(child, page));
            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getPages())
                .usingRecursiveComparison()
                .isEqualTo(
                    List.of(
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("swagger")
                            .type(PageType.SWAGGER)
                            .name("swagger")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId("parent-id")
                            .published(true)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id(page.getId())
                            .type(PageType.FOLDER)
                            .name(page.getName())
                            .order(page.getOrder())
                            .published(page.isPublished())
                            .homepage(page.isHomepage())
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId(page.getParentId())
                            .hidden(false)
                            .build()
                    )
                );
        }

        @Test
        void should_return_async_api_with_published_children() {
            Page child = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.ASYNCAPI)
                .id("async-api")
                .name("async-api")
                .parentId("parent-id")
                .published(true)
                .build();
            Page page = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .id("parent-id")
                .name("folder 2")
                .parentId("")
                .build();
            givenApiPagesQuery(List.of(child, page));
            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getPages())
                .usingRecursiveComparison()
                .isEqualTo(
                    List.of(
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("async-api")
                            .type(PageType.ASYNCAPI)
                            .name("async-api")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId("parent-id")
                            .published(true)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id(page.getId())
                            .type(PageType.FOLDER)
                            .name(page.getName())
                            .order(page.getOrder())
                            .published(page.isPublished())
                            .homepage(page.isHomepage())
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .parentId(page.getParentId())
                            .hidden(false)
                            .build()
                    )
                );
        }

        @Test
        void should_return_pages_with_general_conditions_indicated() {
            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("page-1")
                .name("page-1")
                .build();
            Page page2 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.SWAGGER)
                .id("swagger-1")
                .name("swagger-1")
                .build();
            Page page3 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.SWAGGER)
                .id("swagger-2")
                .name("swagger-2")
                .build();
            Page page4 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("page-2")
                .name("page-2")
                .build();
            givenApiPagesQuery(List.of(page1, page2, page3, page4));
            planQueryServiceInMemory.initWith(
                List.of(
                    PlanFixtures
                        .aPlanV4()
                        .toBuilder()
                        .id("plan-1")
                        .apiId("api-id")
                        .generalConditions("page-1")
                        .build()
                        .setPlanStatus(PlanStatus.PUBLISHED)
                )
            );

            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(ApiDocumentationPagesResponse.class);
            assertThat(body.getBreadcrumb()).isNull();
            assertThat(body.getPages())
                .isEqualTo(
                    List.of(
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("page-1")
                            .type(PageType.MARKDOWN)
                            .name("page-1")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(true)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("swagger-1")
                            .type(PageType.SWAGGER)
                            .name("swagger-1")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("swagger-2")
                            .type(PageType.SWAGGER)
                            .name("swagger-2")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(false)
                            .build(),
                        io.gravitee.rest.api.management.v2.rest.model.Page
                            .builder()
                            .id("page-2")
                            .type(PageType.MARKDOWN)
                            .name("page-2")
                            .order(0)
                            .published(false)
                            .homepage(false)
                            .configuration(Map.of())
                            .metadata(Map.of())
                            .excludedAccessControls(false)
                            .generalConditions(false)
                            .build()
                    )
                );
        }

        @Test
        void should_throw_error_if_parent_not_folder() {
            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("page-1")
                .name("page-1")
                .parentId("parent-id")
                .build();
            Page parent = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .id("parent-id")
                .name("markdown")
                .parentId("")
                .build();
            givenApiPagesQuery(List.of(page1, parent));
            final Response response = rootTarget().queryParam("parentId", "parent-id").request().get();

            MAPIAssertions.assertThat(response).hasStatus(400).asError().hasHttpStatus(400).hasMessage("Page parent must be a FOLDER.");
        }
    }

    @Nested
    class CreateDocumentationTest {

        @BeforeEach
        void setUp() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));
            membershipQueryService.initWith(
                List.of(
                    Membership
                        .builder()
                        .id("member-id")
                        .memberId("my-member-id")
                        .memberType(Membership.Type.USER)
                        .referenceType(Membership.ReferenceType.API)
                        .referenceId(API_ID)
                        .roleId(ROLE_ID)
                        .build()
                )
            );
            userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").build()));
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_DOCUMENTATION),
                    eq("api-id"),
                    eq(RolePermissionAction.CREATE)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().request().post(Entity.json(CreateDocumentation.builder().build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_create_markdown_page() {
            var pageToCreate = CreateDocumentationMarkdown
                .builder()
                .name("created page")
                .homepage(true)
                .content("nice content")
                .type(CreateDocumentation.TypeEnum.MARKDOWN)
                .parentId(null)
                .visibility(Visibility.PUBLIC)
                .excludedAccessControls(true)
                .accessControls(MAPI_V2_ACCESS_ROLES)
                .build();

            final Response response = rootTarget().request().post(Entity.json(pageToCreate));
            var createdPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(createdPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("type", PageType.MARKDOWN)
                .hasFieldOrPropertyWithValue("name", pageToCreate.getName())
                .hasFieldOrPropertyWithValue("homepage", pageToCreate.getHomepage())
                .hasFieldOrPropertyWithValue("content", pageToCreate.getContent())
                .hasFieldOrPropertyWithValue("order", 0)
                .hasFieldOrPropertyWithValue("parentId", pageToCreate.getParentId())
                .hasFieldOrPropertyWithValue("visibility", pageToCreate.getVisibility())
                .hasFieldOrPropertyWithValue("excludedAccessControls", pageToCreate.getExcludedAccessControls())
                .satisfies(page -> assertThat(page.getAccessControls()).hasSize(2).containsAll(pageToCreate.getAccessControls()));

            assertThat(createdPage.getId()).isNotNull();
            assertThat(createdPage.getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_create_swagger_page() {
            var pageToCreate = CreateDocumentationSwagger
                .builder()
                .name("created page")
                .homepage(true)
                .content("openapi: 3.0.0")
                .type(CreateDocumentation.TypeEnum.SWAGGER)
                .parentId(null)
                .visibility(Visibility.PUBLIC)
                .build();

            final Response response = rootTarget().request().post(Entity.json(pageToCreate));
            var createdPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(createdPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("type", PageType.SWAGGER)
                .hasFieldOrPropertyWithValue("name", pageToCreate.getName())
                .hasFieldOrPropertyWithValue("homepage", pageToCreate.getHomepage())
                .hasFieldOrPropertyWithValue("content", pageToCreate.getContent())
                .hasFieldOrPropertyWithValue("order", 0)
                .hasFieldOrPropertyWithValue("parentId", pageToCreate.getParentId())
                .hasFieldOrPropertyWithValue("visibility", pageToCreate.getVisibility());

            assertThat(createdPage.getId()).isNotNull();
            assertThat(createdPage.getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_create_async_api_page() {
            var pageToCreate = CreateDocumentationAsyncApi
                .builder()
                .name("created page")
                .homepage(true)
                .content("nice content")
                .type(CreateDocumentation.TypeEnum.ASYNCAPI)
                .parentId(null)
                .visibility(Visibility.PUBLIC)
                .build();

            final Response response = rootTarget().request().post(Entity.json(pageToCreate));
            var createdPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(createdPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("type", PageType.ASYNCAPI)
                .hasFieldOrPropertyWithValue("name", pageToCreate.getName())
                .hasFieldOrPropertyWithValue("homepage", pageToCreate.getHomepage())
                .hasFieldOrPropertyWithValue("content", pageToCreate.getContent())
                .hasFieldOrPropertyWithValue("order", 0)
                .hasFieldOrPropertyWithValue("parentId", pageToCreate.getParentId())
                .hasFieldOrPropertyWithValue("visibility", pageToCreate.getVisibility());

            assertThat(createdPage.getId()).isNotNull();
            assertThat(createdPage.getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_create_folder() {
            var folderToCreate = CreateDocumentationFolder
                .builder()
                .name("created page")
                .type(CreateDocumentation.TypeEnum.FOLDER)
                .parentId(null)
                .visibility(Visibility.PUBLIC)
                .build();

            final Response response = rootTarget().request().post(Entity.json(folderToCreate));
            var createdPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(createdPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("type", PageType.FOLDER)
                .hasFieldOrPropertyWithValue("name", folderToCreate.getName())
                .hasFieldOrPropertyWithValue("order", 0)
                .hasFieldOrPropertyWithValue("parentId", folderToCreate.getParentId())
                .hasFieldOrPropertyWithValue("visibility", folderToCreate.getVisibility());

            assertThat(createdPage.getId()).isNotNull();
            assertThat(createdPage.getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_not_allow_null_name() {
            var request = CreateDocumentationMarkdown
                .builder()
                .type(CreateDocumentation.TypeEnum.MARKDOWN)
                .parentId("parent")
                .visibility(Visibility.PRIVATE)
                .name(null)
                .build();

            final Response response = rootTarget().request().post(Entity.json(request));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }

        @Test
        public void should_not_allow_empty_name() {
            var request = CreateDocumentationMarkdown
                .builder()
                .type(CreateDocumentation.TypeEnum.MARKDOWN)
                .parentId("parent")
                .visibility(Visibility.PRIVATE)
                .name("")
                .build();

            final Response response = rootTarget().request().post(Entity.json(request));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }
    }

    @Nested
    class GetApiPageTest {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_DOCUMENTATION),
                    eq(API_ID),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().path(PAGE_ID).request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_404_if_api_does_not_exist() {
            final Response response = rootTarget().path(PAGE_ID).request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api not found.");
        }

        @Test
        public void should_return_404_if_page_does_not_exist() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            final Response response = rootTarget().path(PAGE_ID).request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Page [" + PAGE_ID + "] cannot be found.");
        }

        @Test
        void should_get_page() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .type(Page.Type.MARKDOWN)
                .id(PAGE_ID)
                .name("page-1")
                .build();
            givenApiPagesQuery(List.of(page1));
            final Response response = rootTarget().path(PAGE_ID).request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);
            assertThat(body)
                .isEqualTo(
                    io.gravitee.rest.api.management.v2.rest.model.Page
                        .builder()
                        .id(PAGE_ID)
                        .type(PageType.MARKDOWN)
                        .name("page-1")
                        .order(0)
                        .published(false)
                        .homepage(false)
                        .configuration(Map.of())
                        .metadata(Map.of())
                        .excludedAccessControls(false)
                        .generalConditions(false)
                        .build()
                );
        }
    }

    @Nested
    class UpdateDocumentationPage {

        private static final String API_ID = "api-id";
        private static final String PAGE_ID = "page-id";

        @BeforeEach
        void setUp() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));
            roleQueryService.initWith(
                List.of(
                    io.gravitee.apim.core.membership.model.Role
                        .builder()
                        .id(ROLE_ID)
                        .scope(io.gravitee.apim.core.membership.model.Role.Scope.API)
                        .referenceType(io.gravitee.apim.core.membership.model.Role.ReferenceType.ORGANIZATION)
                        .referenceId(ORGANIZATION)
                        .name("PRIMARY_OWNER")
                        .build()
                )
            );
            membershipQueryService.initWith(
                List.of(
                    Membership
                        .builder()
                        .id("member-id")
                        .memberId("my-member-id")
                        .memberType(Membership.Type.USER)
                        .referenceType(Membership.ReferenceType.API)
                        .referenceId(API_ID)
                        .roleId(ROLE_ID)
                        .build()
                )
            );
            userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").build()));
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_DOCUMENTATION),
                    eq(API_ID),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(UpdateDocumentation.builder().build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_update_markdown_page() {
            var request = UpdateDocumentationMarkdown
                .builder()
                .name("created page")
                .homepage(true)
                .content("nice content")
                .type(UpdateDocumentation.TypeEnum.MARKDOWN)
                .order(1)
                .visibility(Visibility.PUBLIC)
                .excludedAccessControls(true)
                .accessControls(MAPI_V2_ACCESS_ROLES)
                .build();
            var oldMarkdown = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("old name")
                .content("old content")
                .visibility(Page.Visibility.PRIVATE)
                .order(2)
                .published(false)
                .type(Page.Type.MARKDOWN)
                .homepage(false)
                .excludedAccessControls(false)
                .accessControls(
                    Set.of(
                        io.gravitee.apim.core.documentation.model.AccessControl
                            .builder()
                            .referenceId("group-2")
                            .referenceType("GROUP")
                            .build()
                    )
                )
                .build();
            givenApiPagesQuery(List.of(oldMarkdown));

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            var createdPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(createdPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("published", false)
                .hasFieldOrPropertyWithValue("type", PageType.MARKDOWN)
                .hasFieldOrPropertyWithValue("name", request.getName())
                .hasFieldOrPropertyWithValue("homepage", request.getHomepage())
                .hasFieldOrPropertyWithValue("content", request.getContent())
                .hasFieldOrPropertyWithValue("order", request.getOrder())
                .hasFieldOrPropertyWithValue("visibility", request.getVisibility())
                .hasFieldOrPropertyWithValue("excludedAccessControls", request.getExcludedAccessControls())
                .satisfies(page -> {
                    assertThat(page.getAccessControls()).hasSize(2).containsAll(MAPI_V2_ACCESS_ROLES);
                });

            assertThat(createdPage.getId()).isNotNull();
            assertThat(createdPage.getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_update_swagger_page() {
            var request = UpdateDocumentationSwagger
                .builder()
                .name("created page")
                .homepage(true)
                .content("openapi: 3.0.0")
                .type(UpdateDocumentation.TypeEnum.SWAGGER)
                .order(1)
                .visibility(Visibility.PUBLIC)
                .build();
            var oldPage = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("old name")
                .content("openapi: 3.0.0")
                .visibility(Page.Visibility.PRIVATE)
                .order(2)
                .published(false)
                .type(Page.Type.SWAGGER)
                .homepage(false)
                .build();
            givenApiPagesQuery(List.of(oldPage));

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            var createdPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(createdPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("published", false)
                .hasFieldOrPropertyWithValue("type", PageType.SWAGGER)
                .hasFieldOrPropertyWithValue("name", request.getName())
                .hasFieldOrPropertyWithValue("homepage", request.getHomepage())
                .hasFieldOrPropertyWithValue("content", request.getContent())
                .hasFieldOrPropertyWithValue("order", request.getOrder())
                .hasFieldOrPropertyWithValue("visibility", request.getVisibility());

            assertThat(createdPage.getId()).isNotNull();
            assertThat(createdPage.getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_update_async_api_page() {
            var request = UpdateDocumentationAsyncApi
                .builder()
                .name("created page")
                .homepage(true)
                .content("nice content")
                .type(UpdateDocumentation.TypeEnum.ASYNCAPI)
                .order(1)
                .visibility(Visibility.PUBLIC)
                .build();
            var oldPage = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("old name")
                .content("old content")
                .visibility(Page.Visibility.PRIVATE)
                .order(2)
                .published(false)
                .type(Page.Type.ASYNCAPI)
                .homepage(false)
                .build();
            givenApiPagesQuery(List.of(oldPage));

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            var createdPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(createdPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("published", false)
                .hasFieldOrPropertyWithValue("type", PageType.ASYNCAPI)
                .hasFieldOrPropertyWithValue("name", request.getName())
                .hasFieldOrPropertyWithValue("homepage", request.getHomepage())
                .hasFieldOrPropertyWithValue("content", request.getContent())
                .hasFieldOrPropertyWithValue("order", request.getOrder())
                .hasFieldOrPropertyWithValue("visibility", request.getVisibility());

            assertThat(createdPage.getId()).isNotNull();
            assertThat(createdPage.getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_update_folder() {
            var folderToCreate = UpdateDocumentationFolder
                .builder()
                .name("new name")
                .type(UpdateDocumentation.TypeEnum.FOLDER)
                .order(24)
                .visibility(Visibility.PUBLIC)
                .build();
            var oldFolder = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("old name")
                .visibility(Page.Visibility.PRIVATE)
                .order(2)
                .published(false)
                .type(Page.Type.FOLDER)
                .build();
            givenApiPagesQuery(List.of(oldFolder));

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(folderToCreate));
            var updatedPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(updatedPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("published", false)
                .hasFieldOrPropertyWithValue("type", PageType.FOLDER)
                .hasFieldOrPropertyWithValue("name", folderToCreate.getName())
                .hasFieldOrPropertyWithValue("order", folderToCreate.getOrder())
                .hasFieldOrPropertyWithValue("visibility", folderToCreate.getVisibility())
                .hasFieldOrPropertyWithValue("type", PageType.FOLDER);

            assertThat(updatedPage.getId()).isNotNull();
            assertThat(updatedPage.getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_change_markdown_page_order_to_0() {
            var request = UpdateDocumentationMarkdown
                .builder()
                .name("created page")
                .homepage(true)
                .content("nice content")
                .type(UpdateDocumentation.TypeEnum.MARKDOWN)
                .order(0)
                .visibility(Visibility.PUBLIC)
                .build();

            var oldMarkdown = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("old name")
                .content("old content")
                .visibility(Page.Visibility.PRIVATE)
                .order(2)
                .published(false)
                .type(Page.Type.MARKDOWN)
                .homepage(false)
                .build();
            givenApiPagesQuery(List.of(oldMarkdown));

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            var updatedPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(updatedPage).isNotNull().hasFieldOrPropertyWithValue("order", request.getOrder());
        }

        @Test
        public void should_change_swagger_page_order_to_0() {
            var request = UpdateDocumentationSwagger
                .builder()
                .name("created page")
                .homepage(true)
                .content("openapi: 3.0.0")
                .type(UpdateDocumentation.TypeEnum.SWAGGER)
                .order(0)
                .visibility(Visibility.PUBLIC)
                .build();

            var oldMarkdown = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("old name")
                .content("openapi: 3.0.0")
                .visibility(Page.Visibility.PRIVATE)
                .order(2)
                .published(false)
                .type(Page.Type.SWAGGER)
                .homepage(false)
                .build();
            givenApiPagesQuery(List.of(oldMarkdown));

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            var updatedPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(updatedPage).isNotNull().hasFieldOrPropertyWithValue("order", request.getOrder());
        }

        @Test
        public void should_update_configuration() {
            var configuration = Map.of("key", "value");
            var request = UpdateDocumentationMarkdown
                .builder()
                .name("created page")
                .homepage(true)
                .content("nice content")
                .type(UpdateDocumentation.TypeEnum.MARKDOWN)
                .order(1)
                .visibility(Visibility.PUBLIC)
                .configuration(configuration)
                .build();
            var oldMarkdown = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("old name")
                .content("old content")
                .visibility(Page.Visibility.PRIVATE)
                .order(2)
                .published(false)
                .type(Page.Type.MARKDOWN)
                .homepage(false)
                .build();
            givenApiPagesQuery(List.of(oldMarkdown));

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            var updatedPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(updatedPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("configuration", configuration);
        }

        @Test
        public void should_update_page_source() {
            var pageSource = PageSource.builder().type("http-fetcher").configuration("{ \"some\": \"config\"}").build();
            var request = UpdateDocumentationMarkdown
                .builder()
                .name("created page")
                .homepage(true)
                .content("nice content")
                .type(UpdateDocumentation.TypeEnum.MARKDOWN)
                .order(1)
                .visibility(Visibility.PUBLIC)
                .source(pageSource)
                .build();
            var oldMarkdown = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("old name")
                .content("old content")
                .visibility(Page.Visibility.PRIVATE)
                .order(2)
                .published(false)
                .type(Page.Type.MARKDOWN)
                .homepage(false)
                .build();
            givenApiPagesQuery(List.of(oldMarkdown));

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            var updatedPage = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);

            assertThat(updatedPage)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .extracting(io.gravitee.rest.api.management.v2.rest.model.Page::getSource)
                .hasFieldOrPropertyWithValue("type", "http-fetcher")
                .extracting(PageSource::getConfiguration)
                .hasFieldOrPropertyWithValue("some", "config");
        }

        @Test
        public void should_not_allow_null_name() {
            var request = UpdateDocumentationMarkdown
                .builder()
                .type(UpdateDocumentation.TypeEnum.MARKDOWN)
                .order(1)
                .visibility(Visibility.PRIVATE)
                .name(null)
                .build();

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }

        @Test
        public void should_not_allow_empty_name() {
            var request = UpdateDocumentationMarkdown
                .builder()
                .type(UpdateDocumentation.TypeEnum.MARKDOWN)
                .order(1)
                .visibility(Visibility.PRIVATE)
                .name("")
                .build();

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }

        @Test
        public void should_not_allow_empty_name_swagger_page() {
            var request = UpdateDocumentationSwagger
                .builder()
                .type(UpdateDocumentation.TypeEnum.SWAGGER)
                .order(1)
                .visibility(Visibility.PRIVATE)
                .name("")
                .build();

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }

        @Test
        public void should_not_allow_empty_name_assync_api_page() {
            var request = UpdateDocumentationSwagger
                .builder()
                .type(UpdateDocumentation.TypeEnum.ASYNCAPI)
                .order(1)
                .visibility(Visibility.PRIVATE)
                .name("")
                .build();

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }

        @Test
        public void should_not_allow_negative_order() {
            var request = UpdateDocumentationMarkdown
                .builder()
                .type(UpdateDocumentation.TypeEnum.MARKDOWN)
                .visibility(Visibility.PRIVATE)
                .name("name")
                .order(-1)
                .build();

            final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }
    }

    @Nested
    class PublishDocumentationPage {

        private static final String PATH = PAGE_ID + "/_publish";

        @Test
        public void should_return_403_if_incorrect_permissions() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_DOCUMENTATION),
                    eq(API_ID),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().path(PATH).request().post(Entity.json(""));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_404_if_api_does_not_exist() {
            final Response response = rootTarget().path(PATH).request().post(Entity.json(""));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api not found.");
        }

        @Test
        public void should_return_404_if_page_does_not_exist() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            final Response response = rootTarget().path(PATH).request().post(Entity.json(""));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Page [" + PAGE_ID + "] cannot be found.");
        }

        @Test
        void should_publish_page() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .type(Page.Type.MARKDOWN)
                .id(PAGE_ID)
                .name("page-1")
                .published(false)
                .build();
            givenApiPagesQuery(List.of(page1));
            final Response response = rootTarget().path(PATH).request().post(Entity.json(""));
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);
            assertThat(body).isNotNull().hasFieldOrPropertyWithValue("published", true);
        }
    }

    @Nested
    class UnpublishDocumentationPage {

        private static final String PATH = PAGE_ID + "/_unpublish";

        @Test
        public void should_return_403_if_incorrect_permissions() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_DOCUMENTATION),
                    eq(API_ID),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().path(PATH).request().post(Entity.json(""));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_404_if_api_does_not_exist() {
            final Response response = rootTarget().path(PATH).request().post(Entity.json(""));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api not found.");
        }

        @Test
        public void should_return_404_if_page_does_not_exist() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            final Response response = rootTarget().path(PATH).request().post(Entity.json(""));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Page [" + PAGE_ID + "] cannot be found.");
        }

        @Test
        void should_unpublish_page() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .type(Page.Type.MARKDOWN)
                .id(PAGE_ID)
                .name("page-1")
                .published(true)
                .build();
            givenApiPagesQuery(List.of(page1));
            final Response response = rootTarget().path(PATH).request().post(Entity.json(""));
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Page.class);
            assertThat(body).isNotNull().hasFieldOrPropertyWithValue("published", false);
        }
    }

    @Nested
    class DeleteDocumentationPage {

        private static final String PATH = PAGE_ID;

        @Test
        void should_return_403_if_incorrect_permissions() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_DOCUMENTATION),
                    eq(API_ID),
                    eq(RolePermissionAction.DELETE)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().path(PATH).request().delete();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_return_404_if_api_does_not_exist() {
            final Response response = rootTarget().path(PATH).request().delete();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api not found.");
        }

        @Test
        void should_return_404_if_page_does_not_exist() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            final Response response = rootTarget().path(PATH).request().delete();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Page [" + PAGE_ID + "] cannot be found.");
        }

        @Test
        void should_return_400_if_page_is_not_api_page() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.ENVIRONMENT)
                .referenceId(API_ID)
                .type(Page.Type.MARKDOWN)
                .id(PAGE_ID)
                .name("page-1")
                .published(true)
                .build();
            givenApiPagesQuery(List.of(page1));

            final Response response = rootTarget().path(PATH).request().delete();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Page has not the correct reference type.");
        }

        @ParameterizedTest
        @EnumSource(value = PlanStatus.class, names = { "PUBLISHED", "DEPRECATED" })
        void should_return_400_if_page_is_used_as_general_condition(PlanStatus planStatus) {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .type(Page.Type.MARKDOWN)
                .id(PAGE_ID)
                .name("page-1")
                .published(true)
                .build();
            givenApiPagesQuery(List.of(page1));

            planQueryServiceInMemory.initWith(
                List.of(
                    PlanFixtures
                        .aPlanV4()
                        .toBuilder()
                        .id("plan-id")
                        .apiId(API_ID)
                        .generalConditions(PAGE_ID)
                        .build()
                        .setPlanStatus(planStatus)
                )
            );

            final Response response = rootTarget().path(PATH).request().delete();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Page can not be deleted because used as general condition.");
        }

        @Test
        void should_return_400_if_page_is_a_non_empty_folder() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            Page folder = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .type(Page.Type.FOLDER)
                .id("folder-id")
                .name("page-1")
                .published(true)
                .build();
            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .type(Page.Type.MARKDOWN)
                .id(PAGE_ID)
                .name("page-1")
                .published(true)
                .parentId("folder-id")
                .build();
            givenApiPagesQuery(List.of(folder, page1));

            final Response response = rootTarget().path("folder-id").request().delete();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Folder cannot be deleted as it is not empty.");
        }

        @Test
        void should_delete_page() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));

            Page page1 = Page
                .builder()
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .type(Page.Type.MARKDOWN)
                .id(PAGE_ID)
                .name("page-1")
                .published(true)
                .parentId("folder-id")
                .build();
            givenApiPagesQuery(List.of(page1));

            final Response response = rootTarget().path(PATH).request().delete();

            MAPIAssertions.assertThat(response).hasStatus(NO_CONTENT_204);

            assertThat(pageCrudServiceInMemory.findById(PAGE_ID)).isEmpty();
        }
    }

    private void givenApiPagesQuery(List<Page> pages) {
        pageQueryServiceInMemory.initWith(pages);
        pageCrudServiceInMemory.initWith(pages);
    }
}
