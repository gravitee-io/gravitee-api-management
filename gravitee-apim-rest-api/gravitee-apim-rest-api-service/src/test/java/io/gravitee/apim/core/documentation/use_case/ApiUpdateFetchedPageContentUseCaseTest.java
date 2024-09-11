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
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.exception.ApiPageSourceNotDefinedException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiUpdateFetchedPageContentUseCaseTest {

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

    private static final Page PARENT_FOLDER = Page
        .builder()
        .id(PARENT_ID)
        .referenceType(Page.ReferenceType.API)
        .referenceId(API_ID)
        .parentId("")
        .name("parent")
        .type(Page.Type.FOLDER)
        .build();
    private static final Page MARKDOWN_WITH_SOURCE = Page
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
        .source(PageSource.builder().type("http-fetcher").configuration("{}").build())
        .build();
    private static final Page MARKDOWN_WITH_SOURCE_NO_UPDATE = Page
        .builder()
        .id("markdown-with-no-source-no-update")
        .referenceType(Page.ReferenceType.API)
        .referenceId(API_ID)
        .parentId(PARENT_ID)
        .name("old page")
        .type(Page.Type.MARKDOWN)
        .order(2)
        .createdAt(DATE)
        .updatedAt(DATE)
        .content(PageSourceDomainServiceInMemory.MARKDOWN)
        .crossId("cross id")
        .published(true)
        .homepage(true)
        .visibility(Page.Visibility.PUBLIC)
        .source(PageSource.builder().type("http-fetcher").configuration("{}").build())
        .build();
    private static final Page MARKDOWN_WITH_NO_SOURCE = Page
        .builder()
        .id("no-source")
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
        .source(null)
        .build();
    private static final Page MARKDOWN_TO_OTHER_API = Page
        .builder()
        .id("markdown-to-other-api")
        .referenceType(Page.ReferenceType.API)
        .referenceId("another-api")
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
    ApiUpdateFetchedPageContentUseCase apiUpdateFetchedPageContentUseCase;

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
        apiUpdateFetchedPageContentUseCase =
            new ApiUpdateFetchedPageContentUseCase(
                new ApiDocumentationDomainService(pageQueryService, planQueryService),
                apiCrudService,
                pageCrudService,
                documentationValidationDomainService,
                updateApiDocumentationDomainService
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

        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(PARENT_FOLDER, MARKDOWN_WITH_SOURCE, MARKDOWN_WITH_NO_SOURCE, MARKDOWN_TO_OTHER_API, MARKDOWN_WITH_SOURCE_NO_UPDATE)
        );
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

    @Test
    void should_update_markdown() {
        var res = apiUpdateFetchedPageContentUseCase.execute(
            ApiUpdateFetchedPageContentUseCase.Input
                .builder()
                .apiId(API_ID)
                .pageId(MARKDOWN_WITH_SOURCE.getId())
                .auditInfo(AUDIT_INFO)
                .build()
        );

        assertThat(res.page())
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", PAGE_ID)
            .hasFieldOrPropertyWithValue("content", PageSourceDomainServiceInMemory.MARKDOWN);
    }

    @Test
    void should_create_audit() {
        apiUpdateFetchedPageContentUseCase.execute(
            ApiUpdateFetchedPageContentUseCase.Input
                .builder()
                .apiId(API_ID)
                .pageId(MARKDOWN_WITH_SOURCE.getId())
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
    void should_create_a_page_revision_if_content_fetched() {
        apiUpdateFetchedPageContentUseCase.execute(
            ApiUpdateFetchedPageContentUseCase.Input
                .builder()
                .apiId(API_ID)
                .pageId(MARKDOWN_WITH_SOURCE.getId())
                .auditInfo(AUDIT_INFO)
                .build()
        );

        var pageRevision = pageRevisionCrudService.storage().get(0);
        assertThat(pageRevision)
            .isNotNull()
            .hasFieldOrPropertyWithValue("pageId", PAGE_ID)
            .hasFieldOrPropertyWithValue("contributor", null)
            .hasFieldOrPropertyWithValue("name", "old page")
            .hasFieldOrPropertyWithValue("content", PageSourceDomainServiceInMemory.MARKDOWN);
    }

    @Test
    void should_not_create_a_page_revision_if_content_the_same() {
        apiUpdateFetchedPageContentUseCase.execute(
            ApiUpdateFetchedPageContentUseCase.Input
                .builder()
                .apiId(API_ID)
                .pageId(MARKDOWN_WITH_SOURCE_NO_UPDATE.getId())
                .auditInfo(AUDIT_INFO)
                .build()
        );

        assertThat(pageRevisionCrudService.storage()).hasSize(0);
    }

    @Test
    void should_throw_error_if_no_source() {
        assertThatThrownBy(() ->
                apiUpdateFetchedPageContentUseCase.execute(
                    ApiUpdateFetchedPageContentUseCase.Input
                        .builder()
                        .apiId(API_ID)
                        .pageId(MARKDOWN_WITH_NO_SOURCE.getId())
                        .auditInfo(AUDIT_INFO)
                        .build()
                )
            )
            .isInstanceOf(ApiPageSourceNotDefinedException.class);
    }

    @Test
    void should_throw_error_if_api_not_found() {
        assertThatThrownBy(() ->
                apiUpdateFetchedPageContentUseCase.execute(
                    ApiUpdateFetchedPageContentUseCase.Input
                        .builder()
                        .apiId("unknown-api-id")
                        .pageId(MARKDOWN_WITH_SOURCE.getId())
                        .auditInfo(AUDIT_INFO)
                        .build()
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_error_if_page_not_found() {
        assertThatThrownBy(() ->
                apiUpdateFetchedPageContentUseCase.execute(
                    ApiUpdateFetchedPageContentUseCase.Input.builder().apiId(API_ID).pageId("unknown").auditInfo(AUDIT_INFO).build()
                )
            )
            .isInstanceOf(PageNotFoundException.class);
    }

    @Test
    void should_throw_error_if_page_not_associated_to_api() {
        assertThatThrownBy(() ->
                apiUpdateFetchedPageContentUseCase.execute(
                    ApiUpdateFetchedPageContentUseCase.Input
                        .builder()
                        .apiId(API_ID)
                        .pageId(MARKDOWN_TO_OTHER_API.getId())
                        .auditInfo(AUDIT_INFO)
                        .build()
                )
            )
            .isInstanceOf(ValidationDomainException.class);
    }

    private void initPageServices(List<Page> pages) {
        pageQueryService.initWith(pages);
        pageCrudService.initWith(pages);
    }

    private void initApiServices(List<Api> apis) {
        apiCrudService.initWith(apis);
    }
}
