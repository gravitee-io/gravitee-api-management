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
package io.gravitee.apim.core.documentation.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.*;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.documentation.domain_service.HomepageDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import io.gravitee.apim.core.documentation.exception.InvalidPageNameException;
import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ApiUpdateDocumentationPageUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String API_ID = "api-id";
    private static final String PARENT_ID = "parent-id";
    private static final String PAGE_ID = "page-id";
    private static final Date DATE = new Date();
    private static final String ROLE_ID = "role-id";
    private static final String GROUP_ID = "group-id";
    private static final Set<AccessControl> ACCESS_CONTROLS = Set.of(
        AccessControl.builder().referenceId(ROLE_ID).referenceType("ROLE").build(),
        AccessControl.builder().referenceId(GROUP_ID).referenceType("GROUP").build()
    );
    private static final Page PARENT_FOLDER = Page
        .builder()
        .id(PARENT_ID)
        .referenceType(Page.ReferenceType.API)
        .referenceId(API_ID)
        .parentId("")
        .name("parent")
        .type(Page.Type.FOLDER)
        .build();
    private static final Page OLD_MARKDOWN_PAGE = Page
        .builder()
        .id(PAGE_ID)
        .referenceType(Page.ReferenceType.API)
        .referenceId(API_ID)
        .parentId(PARENT_ID)
        .name("old page")
        .type(Page.Type.MARKDOWN)
        .order(2)
        .createdAt(DATE)
        .updatedAt(DATE)
        .content("old content")
        .crossId("cross id")
        .published(true)
        .homepage(true)
        .visibility(Page.Visibility.PUBLIC)
        .build();

    private static final Page OLD_SWAGGER_PAGE = Page
        .builder()
        .id(PAGE_ID)
        .referenceType(Page.ReferenceType.API)
        .referenceId(API_ID)
        .parentId(PARENT_ID)
        .name("old page")
        .type(Page.Type.SWAGGER)
        .order(2)
        .createdAt(DATE)
        .updatedAt(DATE)
        .content("openapi: 3.0.0")
        .crossId("cross id")
        .published(true)
        .homepage(true)
        .visibility(Page.Visibility.PUBLIC)
        .build();
    private static final Page OLD_ASYNC_API_PAGE = Page
        .builder()
        .id(PAGE_ID)
        .referenceType(Page.ReferenceType.API)
        .referenceId(API_ID)
        .parentId(PARENT_ID)
        .name("old page")
        .type(Page.Type.ASYNCAPI)
        .order(2)
        .createdAt(DATE)
        .updatedAt(DATE)
        .content("old content")
        .crossId("cross id")
        .published(true)
        .homepage(true)
        .visibility(Page.Visibility.PUBLIC)
        .build();

    private static final Page OLD_FOLDER_PAGE = Page
        .builder()
        .id(PAGE_ID)
        .referenceType(Page.ReferenceType.API)
        .referenceId(API_ID)
        .parentId(PARENT_ID)
        .name("old folder")
        .type(Page.Type.FOLDER)
        .order(2)
        .createdAt(DATE)
        .updatedAt(DATE)
        .crossId("cross id")
        .published(true)
        .homepage(false)
        .visibility(Page.Visibility.PUBLIC)
        .build();

    private final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    private final PageSourceDomainServiceInMemory pageSourceDomainService = new PageSourceDomainServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final IndexerInMemory indexer = new IndexerInMemory();

    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    DocumentationValidationDomainService documentationValidationDomainService;
    UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    ApiUpdateDocumentationPageUseCase apiUpdateDocumentationPageUsecase;

    @BeforeEach
    void setUp() {
        documentationValidationDomainService =
            new DocumentationValidationDomainService(
                new HtmlSanitizerImpl(),
                new NoopTemplateResolverDomainService(),
                apiCrudService,
                new NoopSwaggerOpenApiResolver(),
                new ApiMetadataQueryServiceInMemory(),
                new ApiPrimaryOwnerDomainService(
                    new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
                    groupQueryService,
                    membershipCrudService,
                    membershipQueryService,
                    roleQueryService,
                    userCrudService
                ),
                new ApiDocumentationDomainService(pageQueryService, planQueryService),
                pageCrudService,
                pageSourceDomainService,
                groupQueryService,
                roleQueryService
            );

        updateApiDocumentationDomainService =
            new UpdateApiDocumentationDomainService(
                pageCrudService,
                pageRevisionCrudService,
                new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
                indexer
            );
        apiUpdateDocumentationPageUsecase =
            new ApiUpdateDocumentationPageUseCase(
                updateApiDocumentationDomainService,
                new ApiDocumentationDomainService(pageQueryService, planQueryService),
                new HomepageDomainService(pageQueryService, pageCrudService),
                apiCrudService,
                pageCrudService,
                pageQueryService,
                documentationValidationDomainService
            );
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build()));
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .id(ROLE_ID)
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
        groupQueryService.initWith(List.of(Group.builder().id(GROUP_ID).build()));
        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId("role-id")
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").build()));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                pageQueryService,
                pageCrudService,
                pageRevisionCrudService,
                auditCrudService,
                userCrudService,
                roleQueryService,
                membershipQueryService,
                groupQueryService
            )
            .forEach(InMemoryAlternative::reset);
    }

    @Nested
    class UpdateMarkdownTests {

        @Test
        void should_update_markdown() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(Page.Visibility.PRIVATE)
                    .content("new content")
                    .name("new name   ")
                    .auditInfo(AUDIT_INFO)
                    .excludedAccessControls(true)
                    .accessControls(ACCESS_CONTROLS)
                    .build()
            );

            assertThat(res.page())
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("name", "new name")
                .hasFieldOrPropertyWithValue("type", Page.Type.MARKDOWN)
                .hasFieldOrPropertyWithValue("content", "new content")
                .hasFieldOrPropertyWithValue("homepage", false)
                .hasFieldOrPropertyWithValue("visibility", Page.Visibility.PRIVATE)
                .hasFieldOrPropertyWithValue("parentId", "parent-id")
                .hasFieldOrPropertyWithValue("order", 24)
                .hasFieldOrPropertyWithValue("crossId", "cross id")
                .hasFieldOrPropertyWithValue("parentId", PARENT_ID)
                .hasFieldOrPropertyWithValue("published", true)
                .hasFieldOrPropertyWithValue("createdAt", DATE)
                .hasFieldOrPropertyWithValue("excludedAccessControls", true)
                .extracting(Page::getAccessControls)
                .isNotNull()
                .isEqualTo(ACCESS_CONTROLS);

            assertThat(res.page().getUpdatedAt()).isNotEqualTo(DATE);
        }

        @Test
        void should_create_audit() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(Page.Visibility.PRIVATE)
                    .content("new content")
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", API_ID)
                .hasFieldOrPropertyWithValue("event", "PAGE_UPDATED");
        }

        @Test
        void should_create_a_page_revision_if_name_different() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_MARKDOWN_PAGE.getOrder())
                    .visibility(OLD_MARKDOWN_PAGE.getVisibility())
                    .content(OLD_MARKDOWN_PAGE.getContent())
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var pageRevision = pageRevisionCrudService.storage().get(0);
            assertThat(pageRevision)
                .isNotNull()
                .hasFieldOrPropertyWithValue("pageId", PAGE_ID)
                .hasFieldOrPropertyWithValue("contributor", null)
                .hasFieldOrPropertyWithValue("name", "new name")
                .hasFieldOrPropertyWithValue("content", "old content");
        }

        @Test
        void should_create_a_page_revision_if_content_different() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_MARKDOWN_PAGE.getOrder())
                    .visibility(OLD_MARKDOWN_PAGE.getVisibility())
                    .content("new content")
                    .name(OLD_MARKDOWN_PAGE.getName())
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var pageRevision = pageRevisionCrudService.storage().get(0);
            assertThat(pageRevision)
                .isNotNull()
                .hasFieldOrPropertyWithValue("pageId", PAGE_ID)
                .hasFieldOrPropertyWithValue("contributor", null)
                .hasFieldOrPropertyWithValue("name", "old page")
                .hasFieldOrPropertyWithValue("content", "new content");
        }

        @Test
        void should_not_create_a_page_revision_if_content_and_name_the_same() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(OLD_MARKDOWN_PAGE.getVisibility())
                    .content(OLD_MARKDOWN_PAGE.getContent())
                    .name(OLD_MARKDOWN_PAGE.getName())
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(pageRevisionCrudService.storage()).hasSize(0);
        }

        @Test
        void should_change_homepage_to_new_page() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));

            final String EXISTING_PAGE_ID = "existing-homepage-id";
            var existingHomepage = Page
                .builder()
                .id(EXISTING_PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("existing homepage")
                .homepage(true)
                .build();

            initPageServices(List.of(OLD_MARKDOWN_PAGE.toBuilder().homepage(false).build(), existingHomepage));

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_MARKDOWN_PAGE.getOrder())
                    .visibility(OLD_MARKDOWN_PAGE.getVisibility())
                    .content(OLD_MARKDOWN_PAGE.getContent())
                    .name(OLD_MARKDOWN_PAGE.getName())
                    .homepage(true)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page()).isNotNull().hasFieldOrPropertyWithValue("homepage", true);

            var formerHomepage = pageCrudService.storage().stream().filter(p -> p.getId().equals(EXISTING_PAGE_ID)).toList().get(0);
            assertThat(formerHomepage).isNotNull().hasFieldOrPropertyWithValue("homepage", false);
        }

        @Test
        void should_only_retain_existing_groups_and_roles_in_access_controls() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));

            var accessControlsWithInvalidEntities = new HashSet<>(ACCESS_CONTROLS);
            accessControlsWithInvalidEntities.add(AccessControl.builder().referenceType("USER").referenceId("user-1").build());
            accessControlsWithInvalidEntities.add(AccessControl.builder().referenceType("GROUP").referenceId("does-not-exist").build());
            accessControlsWithInvalidEntities.add(AccessControl.builder().referenceType("ROLE").referenceId("does-not-exist").build());
            accessControlsWithInvalidEntities.add(AccessControl.builder().referenceType("group").referenceId(GROUP_ID).build());
            accessControlsWithInvalidEntities.add(AccessControl.builder().referenceType("role").referenceId(ROLE_ID).build());

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(Page.Visibility.PRIVATE)
                    .content("new content")
                    .name("new name   ")
                    .auditInfo(AUDIT_INFO)
                    .excludedAccessControls(false)
                    .accessControls(accessControlsWithInvalidEntities)
                    .build()
            );

            assertThat(res.page())
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("excludedAccessControls", false)
                .extracting(Page::getAccessControls)
                .isNotNull()
                .isEqualTo(ACCESS_CONTROLS);
        }

        @Test
        void should_update_configuration() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));

            var configuration = Map.of("new", "config");

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .configuration(configuration)
                    .apiId(API_ID)
                    .pageId(OLD_MARKDOWN_PAGE.getId())
                    .order(OLD_MARKDOWN_PAGE.getOrder())
                    .visibility(OLD_MARKDOWN_PAGE.getVisibility())
                    .content(OLD_MARKDOWN_PAGE.getContent())
                    .name(OLD_MARKDOWN_PAGE.getName())
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page())
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("configuration", configuration);
        }

        @Test
        void should_throw_error_if_markdown_unsafe() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));
            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .order(OLD_MARKDOWN_PAGE.getOrder())
                            .visibility(OLD_MARKDOWN_PAGE.getVisibility())
                            .content(getNotSafe())
                            .name(OLD_MARKDOWN_PAGE.getName())
                            .homepage(OLD_MARKDOWN_PAGE.isHomepage())
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(PageContentUnsafeException.class);
        }

        @Test
        void should_throw_error_if_duplicate_name() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            var duplicateName = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("new page")
                .type(Page.Type.MARKDOWN)
                .build();
            initPageServices(List.of(OLD_MARKDOWN_PAGE.toBuilder().parentId(null).build(), duplicateName));
            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .order(OLD_MARKDOWN_PAGE.getOrder())
                            .visibility(OLD_MARKDOWN_PAGE.getVisibility())
                            .content("new content")
                            .name("new page")
                            .homepage(OLD_MARKDOWN_PAGE.isHomepage())
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void should_throw_error_if_api_not_found() {
            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId("unknown-api-id")
                            .pageId(PAGE_ID)
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ApiNotFoundException.class);
        }

        @Test
        void should_throw_error_if_page_not_found() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input.builder().apiId(API_ID).pageId(PAGE_ID).auditInfo(AUDIT_INFO).build()
                    )
                )
                .isInstanceOf(PageNotFoundException.class);
        }

        @Test
        void should_throw_error_if_page_not_associated_to_api() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(OLD_MARKDOWN_PAGE.toBuilder().referenceId("other api").build()));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input.builder().apiId(API_ID).pageId(PAGE_ID).auditInfo(AUDIT_INFO).build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = { "  " })
        @NullAndEmptySource
        void should_throw_error_if_page_name_is_null_or_empty(String name) {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_MARKDOWN_PAGE));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .order(OLD_MARKDOWN_PAGE.getOrder())
                            .visibility(OLD_MARKDOWN_PAGE.getVisibility())
                            .content("new content")
                            .name(name)
                            .homepage(OLD_MARKDOWN_PAGE.isHomepage())
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(InvalidPageNameException.class);
        }
    }

    @Nested
    class UpdateSwaggerTests {

        String newContent = "openapi: 3.0.0\n" + "info:\n" + "  title: Sample API";

        @Test
        void should_update_markdown() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_SWAGGER_PAGE));

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(Page.Visibility.PRIVATE)
                    .content(newContent)
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page())
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("name", "new name")
                .hasFieldOrPropertyWithValue("type", Page.Type.SWAGGER)
                .hasFieldOrPropertyWithValue("content", newContent)
                .hasFieldOrPropertyWithValue("homepage", false)
                .hasFieldOrPropertyWithValue("visibility", Page.Visibility.PRIVATE)
                .hasFieldOrPropertyWithValue("parentId", "parent-id")
                .hasFieldOrPropertyWithValue("order", 24)
                .hasFieldOrPropertyWithValue("crossId", "cross id")
                .hasFieldOrPropertyWithValue("parentId", PARENT_ID)
                .hasFieldOrPropertyWithValue("published", true)
                .hasFieldOrPropertyWithValue("createdAt", DATE);

            assertThat(res.page().getUpdatedAt()).isNotEqualTo(DATE);
        }

        @Test
        void should_create_audit() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_SWAGGER_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(Page.Visibility.PRIVATE)
                    .content(newContent)
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", API_ID)
                .hasFieldOrPropertyWithValue("event", "PAGE_UPDATED");
        }

        @Test
        void should_create_a_page_revision_if_name_different() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_SWAGGER_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_SWAGGER_PAGE.getOrder())
                    .visibility(OLD_SWAGGER_PAGE.getVisibility())
                    .content(OLD_SWAGGER_PAGE.getContent())
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var pageRevision = pageRevisionCrudService.storage().get(0);
            assertThat(pageRevision)
                .isNotNull()
                .hasFieldOrPropertyWithValue("pageId", PAGE_ID)
                .hasFieldOrPropertyWithValue("contributor", null)
                .hasFieldOrPropertyWithValue("name", "new name")
                .hasFieldOrPropertyWithValue("content", "openapi: 3.0.0");
        }

        @Test
        void should_create_a_page_revision_if_content_different() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_SWAGGER_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_SWAGGER_PAGE.getOrder())
                    .visibility(OLD_SWAGGER_PAGE.getVisibility())
                    .content(newContent)
                    .name(OLD_SWAGGER_PAGE.getName())
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var pageRevision = pageRevisionCrudService.storage().get(0);
            assertThat(pageRevision)
                .isNotNull()
                .hasFieldOrPropertyWithValue("pageId", PAGE_ID)
                .hasFieldOrPropertyWithValue("contributor", null)
                .hasFieldOrPropertyWithValue("name", "old page")
                .hasFieldOrPropertyWithValue("content", newContent);
        }

        @Test
        void should_not_create_a_page_revision_if_content_and_name_the_same() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_SWAGGER_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(OLD_SWAGGER_PAGE.getVisibility())
                    .content(OLD_SWAGGER_PAGE.getContent())
                    .name(OLD_SWAGGER_PAGE.getName())
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(pageRevisionCrudService.storage()).hasSize(0);
        }

        @Test
        void should_change_homepage_to_new_page() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));

            final String EXISTING_PAGE_ID = "existing-homepage-id";
            var existingHomepage = Page
                .builder()
                .id(EXISTING_PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("existing homepage")
                .homepage(true)
                .build();

            initPageServices(List.of(OLD_SWAGGER_PAGE.toBuilder().homepage(false).build(), existingHomepage));

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_SWAGGER_PAGE.getOrder())
                    .visibility(OLD_SWAGGER_PAGE.getVisibility())
                    .content(OLD_SWAGGER_PAGE.getContent())
                    .name(OLD_SWAGGER_PAGE.getName())
                    .homepage(true)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page()).isNotNull().hasFieldOrPropertyWithValue("homepage", true);

            var formerHomepage = pageCrudService.storage().stream().filter(p -> p.getId().equals(EXISTING_PAGE_ID)).toList().get(0);
            assertThat(formerHomepage).isNotNull().hasFieldOrPropertyWithValue("homepage", false);
        }

        @Test
        void should_throw_error_if_content_invalid() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_SWAGGER_PAGE));
            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .order(OLD_SWAGGER_PAGE.getOrder())
                            .visibility(OLD_SWAGGER_PAGE.getVisibility())
                            .content(getNotSafe())
                            .name(OLD_SWAGGER_PAGE.getName())
                            .homepage(OLD_SWAGGER_PAGE.isHomepage())
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(InvalidPageContentException.class);
        }

        @Test
        void should_throw_error_if_duplicate_name() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            var duplicateName = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("new page")
                .type(Page.Type.SWAGGER)
                .build();
            initPageServices(List.of(OLD_SWAGGER_PAGE.toBuilder().parentId(null).build(), duplicateName));
            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .order(OLD_SWAGGER_PAGE.getOrder())
                            .visibility(OLD_SWAGGER_PAGE.getVisibility())
                            .content(newContent)
                            .name("new page")
                            .homepage(OLD_SWAGGER_PAGE.isHomepage())
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void should_throw_error_if_api_not_found() {
            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId("unknown-api-id")
                            .pageId(PAGE_ID)
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ApiNotFoundException.class);
        }

        @Test
        void should_throw_error_if_page_not_found() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input.builder().apiId(API_ID).pageId(PAGE_ID).auditInfo(AUDIT_INFO).build()
                    )
                )
                .isInstanceOf(PageNotFoundException.class);
        }

        @Test
        void should_throw_error_if_page_not_associated_to_api() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(OLD_SWAGGER_PAGE.toBuilder().referenceId("other api").build()));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input.builder().apiId(API_ID).pageId(PAGE_ID).auditInfo(AUDIT_INFO).build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = { "  " })
        @NullAndEmptySource
        void should_throw_error_if_page_name_is_null_or_empty(String name) {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_SWAGGER_PAGE));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .order(OLD_SWAGGER_PAGE.getOrder())
                            .visibility(OLD_SWAGGER_PAGE.getVisibility())
                            .content(newContent)
                            .name(name)
                            .homepage(OLD_SWAGGER_PAGE.isHomepage())
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(InvalidPageNameException.class);
        }
    }

    @Nested
    class UpdateAsyncApiTests {

        @Test
        void should_update_markdown() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_ASYNC_API_PAGE));

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(Page.Visibility.PRIVATE)
                    .content("new content")
                    .name("new name   ")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page())
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("name", "new name")
                .hasFieldOrPropertyWithValue("type", Page.Type.ASYNCAPI)
                .hasFieldOrPropertyWithValue("content", "new content")
                .hasFieldOrPropertyWithValue("homepage", false)
                .hasFieldOrPropertyWithValue("visibility", Page.Visibility.PRIVATE)
                .hasFieldOrPropertyWithValue("parentId", "parent-id")
                .hasFieldOrPropertyWithValue("order", 24)
                .hasFieldOrPropertyWithValue("crossId", "cross id")
                .hasFieldOrPropertyWithValue("parentId", PARENT_ID)
                .hasFieldOrPropertyWithValue("published", true)
                .hasFieldOrPropertyWithValue("createdAt", DATE);

            assertThat(res.page().getUpdatedAt()).isNotEqualTo(DATE);
        }

        @Test
        void should_create_audit() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_ASYNC_API_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(Page.Visibility.PRIVATE)
                    .content("new content")
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", API_ID)
                .hasFieldOrPropertyWithValue("event", "PAGE_UPDATED");
        }

        @Test
        void should_create_a_page_revision_if_name_different() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_ASYNC_API_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_ASYNC_API_PAGE.getOrder())
                    .visibility(OLD_ASYNC_API_PAGE.getVisibility())
                    .content(OLD_ASYNC_API_PAGE.getContent())
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var pageRevision = pageRevisionCrudService.storage().get(0);
            assertThat(pageRevision)
                .isNotNull()
                .hasFieldOrPropertyWithValue("pageId", PAGE_ID)
                .hasFieldOrPropertyWithValue("contributor", null)
                .hasFieldOrPropertyWithValue("name", "new name")
                .hasFieldOrPropertyWithValue("content", "old content");
        }

        @Test
        void should_create_a_page_revision_if_content_different() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_ASYNC_API_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_ASYNC_API_PAGE.getOrder())
                    .visibility(OLD_ASYNC_API_PAGE.getVisibility())
                    .content("new content")
                    .name(OLD_ASYNC_API_PAGE.getName())
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var pageRevision = pageRevisionCrudService.storage().get(0);
            assertThat(pageRevision)
                .isNotNull()
                .hasFieldOrPropertyWithValue("pageId", PAGE_ID)
                .hasFieldOrPropertyWithValue("contributor", null)
                .hasFieldOrPropertyWithValue("name", OLD_ASYNC_API_PAGE.getName())
                .hasFieldOrPropertyWithValue("content", "new content");
        }

        @Test
        void should_not_create_a_page_revision_if_content_and_name_the_same() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_ASYNC_API_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(24)
                    .visibility(OLD_ASYNC_API_PAGE.getVisibility())
                    .content(OLD_ASYNC_API_PAGE.getContent())
                    .name(OLD_ASYNC_API_PAGE.getName())
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(pageRevisionCrudService.storage()).hasSize(0);
        }

        @Test
        void should_change_homepage_to_new_page() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));

            final String EXISTING_PAGE_ID = "existing-homepage-id";
            var existingHomepage = Page
                .builder()
                .id(EXISTING_PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("existing homepage")
                .homepage(true)
                .build();

            initPageServices(List.of(OLD_ASYNC_API_PAGE.toBuilder().homepage(false).build(), existingHomepage));

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .order(OLD_ASYNC_API_PAGE.getOrder())
                    .visibility(OLD_ASYNC_API_PAGE.getVisibility())
                    .content(OLD_ASYNC_API_PAGE.getContent())
                    .name(OLD_ASYNC_API_PAGE.getName())
                    .homepage(true)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page()).isNotNull().hasFieldOrPropertyWithValue("homepage", true);

            var formerHomepage = pageCrudService.storage().stream().filter(p -> p.getId().equals(EXISTING_PAGE_ID)).toList().get(0);
            assertThat(formerHomepage).isNotNull().hasFieldOrPropertyWithValue("homepage", false);
        }

        @Test
        void should_throw_error_if_duplicate_name() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            var duplicateName = Page
                .builder()
                .id(PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("new page")
                .type(Page.Type.ASYNCAPI)
                .build();
            initPageServices(List.of(OLD_ASYNC_API_PAGE.toBuilder().parentId(null).build(), duplicateName));
            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .order(OLD_ASYNC_API_PAGE.getOrder())
                            .visibility(OLD_ASYNC_API_PAGE.getVisibility())
                            .content("new content")
                            .name("new page")
                            .homepage(OLD_ASYNC_API_PAGE.isHomepage())
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void should_throw_error_if_api_not_found() {
            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId("unknown-api-id")
                            .pageId(PAGE_ID)
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ApiNotFoundException.class);
        }

        @Test
        void should_throw_error_if_page_not_found() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input.builder().apiId(API_ID).pageId(PAGE_ID).auditInfo(AUDIT_INFO).build()
                    )
                )
                .isInstanceOf(PageNotFoundException.class);
        }

        @Test
        void should_throw_error_if_page_not_associated_to_api() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(OLD_ASYNC_API_PAGE.toBuilder().referenceId("other api").build()));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input.builder().apiId(API_ID).pageId(PAGE_ID).auditInfo(AUDIT_INFO).build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = { "  " })
        @NullAndEmptySource
        void should_throw_error_if_page_name_is_null_or_empty(String name) {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_ASYNC_API_PAGE));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .order(OLD_ASYNC_API_PAGE.getOrder())
                            .visibility(OLD_ASYNC_API_PAGE.getVisibility())
                            .content("new content")
                            .name(name)
                            .homepage(OLD_ASYNC_API_PAGE.isHomepage())
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(InvalidPageNameException.class);
        }
    }

    @Nested
    class UpdateFolderTests {

        @Test
        void should_update_folder() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_FOLDER_PAGE, Page.builder().id("child").parentId(PAGE_ID).published(true).build()));

            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .visibility(Page.Visibility.PRIVATE)
                    .order(24)
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page())
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("name", "new name")
                .hasFieldOrPropertyWithValue("type", Page.Type.FOLDER)
                .hasFieldOrPropertyWithValue("content", null)
                .hasFieldOrPropertyWithValue("homepage", false)
                .hasFieldOrPropertyWithValue("visibility", Page.Visibility.PRIVATE)
                .hasFieldOrPropertyWithValue("parentId", PARENT_ID)
                .hasFieldOrPropertyWithValue("order", 24)
                .hasFieldOrPropertyWithValue("hidden", false)
                .hasFieldOrPropertyWithValue("generalConditions", null);
        }

        @Test
        void should_create_audit() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_FOLDER_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .visibility(Page.Visibility.PRIVATE)
                    .order(24)
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", "api-id")
                .hasFieldOrPropertyWithValue("event", "PAGE_UPDATED");
        }

        @Test
        void should_not_create_page_revision() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_FOLDER_PAGE));

            apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(PAGE_ID)
                    .visibility(Page.Visibility.PRIVATE)
                    .order(24)
                    .name("new name")
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(pageRevisionCrudService.storage()).hasSize(0);
        }

        @Test
        void should_throw_error_if_duplicate_name() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(PARENT_FOLDER, OLD_FOLDER_PAGE, OLD_FOLDER_PAGE.toBuilder().name("new name").build()));

            assertThatThrownBy(() ->
                    apiUpdateDocumentationPageUsecase.execute(
                        ApiUpdateDocumentationPageUseCase.Input
                            .builder()
                            .apiId(API_ID)
                            .pageId(PAGE_ID)
                            .visibility(OLD_FOLDER_PAGE.getVisibility())
                            .order(OLD_FOLDER_PAGE.getOrder())
                            .name("new name")
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class);
        }
    }

    @Nested
    class UpdateOrderTests {

        Page page_0 = Page
            .builder()
            .id("page_0")
            .name("0")
            .type(Page.Type.MARKDOWN)
            .referenceType(Page.ReferenceType.API)
            .referenceId(API_ID)
            .parentId(OLD_FOLDER_PAGE.getId())
            .updatedAt(DATE)
            .order(0)
            .build();
        Page page_1 = Page
            .builder()
            .id("page_1")
            .name("1")
            .type(Page.Type.MARKDOWN)
            .referenceType(Page.ReferenceType.API)
            .referenceId(API_ID)
            .parentId(OLD_FOLDER_PAGE.getId())
            .updatedAt(DATE)
            .order(1)
            .build();
        Page page_2 = Page
            .builder()
            .id("page_2")
            .name("2")
            .type(Page.Type.FOLDER)
            .referenceType(Page.ReferenceType.API)
            .referenceId(API_ID)
            .parentId(OLD_FOLDER_PAGE.getId())
            .updatedAt(DATE)
            .order(2)
            .build();
        Page page_3 = Page
            .builder()
            .id("page_3")
            .name("3")
            .type(Page.Type.MARKDOWN)
            .referenceType(Page.ReferenceType.API)
            .referenceId(API_ID)
            .parentId(OLD_FOLDER_PAGE.getId())
            .updatedAt(DATE)
            .order(3)
            .build();
        Page page_4 = Page
            .builder()
            .id("page_4")
            .name("4")
            .type(Page.Type.MARKDOWN)
            .referenceType(Page.ReferenceType.API)
            .referenceId(API_ID)
            .parentId(OLD_FOLDER_PAGE.getId())
            .updatedAt(DATE)
            .order(4)
            .build();

        @BeforeEach
        void setup() {
            initApiServices(List.of(Api.builder().id(API_ID).build()));
            initPageServices(List.of(OLD_FOLDER_PAGE, page_0, page_1, page_2, page_3, page_4));
        }

        @Test
        void should_insert_new_page_with_lower_order() {
            // Change page_3 order to 1
            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(page_3.getId())
                    .order(1)
                    .visibility(page_3.getVisibility())
                    .content(page_3.getContent())
                    .name(page_3.getName())
                    .homepage(false)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page()).isNotNull().hasFieldOrPropertyWithValue("order", 1);
            assertThat(pageCrudService.get(page_0.getId()).getOrder()).isEqualTo(0);
            assertThat(pageCrudService.get(page_1.getId()).getOrder()).isEqualTo(2);
            assertThat(pageCrudService.get(page_2.getId()).getOrder()).isEqualTo(3);
            assertThat(pageCrudService.get(page_4.getId()).getOrder()).isEqualTo(4);
        }

        @Test
        void should_insert_new_page_with_higher_order() {
            // Change page_1 order to 3
            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(page_1.getId())
                    .order(3)
                    .visibility(page_1.getVisibility())
                    .content(page_1.getContent())
                    .name(page_1.getName())
                    .homepage(false)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page()).isNotNull().hasFieldOrPropertyWithValue("order", 3);
            assertThat(pageCrudService.get(page_0.getId()).getOrder()).isEqualTo(0);
            assertThat(pageCrudService.get(page_2.getId()).getOrder()).isEqualTo(1);
            assertThat(pageCrudService.get(page_3.getId()).getOrder()).isEqualTo(2);
            assertThat(pageCrudService.get(page_4.getId()).getOrder()).isEqualTo(4);
        }

        @Test
        void should_not_change_order_if_the_same() {
            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(page_1.getId())
                    .order(1)
                    .visibility(page_1.getVisibility())
                    .content(page_1.getContent())
                    .name(page_1.getName())
                    .homepage(false)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page()).isNotNull().hasFieldOrPropertyWithValue("order", 1);
            assertThat(pageCrudService.get(page_0.getId()).getOrder()).isEqualTo(0);
            assertThat(pageCrudService.get(page_2.getId()).getOrder()).isEqualTo(2);
            assertThat(pageCrudService.get(page_3.getId()).getOrder()).isEqualTo(3);
            assertThat(pageCrudService.get(page_4.getId()).getOrder()).isEqualTo(4);
        }

        @Test
        void should_update_with_very_high_order() {
            // Change page_1 order to 9999
            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(page_1.getId())
                    .order(9999)
                    .visibility(page_1.getVisibility())
                    .content(page_1.getContent())
                    .name(page_1.getName())
                    .homepage(false)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page()).isNotNull().hasFieldOrPropertyWithValue("order", 9999);
            assertThat(pageCrudService.get(page_0.getId()).getOrder()).isEqualTo(0);
            assertThat(pageCrudService.get(page_2.getId()).getOrder()).isEqualTo(1);
            assertThat(pageCrudService.get(page_3.getId()).getOrder()).isEqualTo(2);
            assertThat(pageCrudService.get(page_4.getId()).getOrder()).isEqualTo(3);
        }

        @Test
        void should_only_change_order_of_pages_with_same_parent() {
            var pageWithDifferentParent = page_0.toBuilder().id("other-page").parentId(null).build();
            initPageServices(List.of(page_0, page_1, page_2, page_3, page_4, pageWithDifferentParent));
            var res = apiUpdateDocumentationPageUsecase.execute(
                ApiUpdateDocumentationPageUseCase.Input
                    .builder()
                    .apiId(API_ID)
                    .pageId(pageWithDifferentParent.getId())
                    .order(1)
                    .visibility(pageWithDifferentParent.getVisibility())
                    .content(pageWithDifferentParent.getContent())
                    .name(pageWithDifferentParent.getName())
                    .homepage(false)
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.page()).isNotNull().hasFieldOrPropertyWithValue("order", 1);
            assertThat(pageCrudService.get(page_0.getId()).getOrder()).isEqualTo(0);
            assertThat(pageCrudService.get(page_1.getId()).getOrder()).isEqualTo(1);
            assertThat(pageCrudService.get(page_2.getId()).getOrder()).isEqualTo(2);
            assertThat(pageCrudService.get(page_3.getId()).getOrder()).isEqualTo(3);
            assertThat(pageCrudService.get(page_4.getId()).getOrder()).isEqualTo(4);
        }
    }

    private void initPageServices(List<Page> pages) {
        pageQueryService.initWith(pages);
        pageCrudService.initWith(pages);
    }

    private void initApiServices(List<Api> apis) {
        apiCrudService.initWith(apis);
    }

    private String getNotSafe() {
        String html = "";
        html += "<script src=\"/external.jpg\" />";
        html += "<div onClick=\"alert('test');\" style=\"margin: auto\">onclick alert<div>";

        return html;
    }
}
