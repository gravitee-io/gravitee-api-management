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
package io.gravitee.apim.core.documentation.domain_service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import inmemory.*;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import io.gravitee.apim.core.documentation.exception.InvalidPageNameException;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
public class DocumentationValidationDomainServiceTest {

    private final String ORGANIZATION_ID = "org-id";
    private final String API_ID = "api-id";

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PageSourceDomainServiceInMemory pageSourceDomainService = new PageSourceDomainServiceInMemory();
    private DocumentationValidationDomainService cut;

    @BeforeEach
    void setUp() {
        var htmlSanitizer = new HtmlSanitizer(new MockEnvironment());

        cut =
            new DocumentationValidationDomainService(
                new HtmlSanitizerImpl(htmlSanitizer),
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
                pageSourceDomainService
            );

        apiCrudService.initWith(List.of(Api.builder().id(API_ID).build()));
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .id("role-id")
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
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
                    .roleId("role-id")
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").build()));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(auditCrudService, userCrudService, roleQueryService, membershipCrudService, userCrudService)
            .forEach(InMemoryAlternative::reset);
    }

    @Nested
    class SanitizeDocumentationName {

        @Test
        void should_throw_an_exception() {
            assertThatThrownBy(() -> cut.sanitizeDocumentationName("")).isInstanceOf(InvalidPageNameException.class);
            assertThatThrownBy(() -> cut.sanitizeDocumentationName("  ")).isInstanceOf(InvalidPageNameException.class);
            assertThatThrownBy(() -> cut.sanitizeDocumentationName(null)).isInstanceOf(InvalidPageNameException.class);
        }

        @Test
        void should_sanitize_name() {
            assertThat(cut.sanitizeDocumentationName("foo")).isEqualTo("foo");
            assertThat(cut.sanitizeDocumentationName("bar     ")).isEqualTo("bar");
        }
    }

    @Nested
    class ValidateContentIsSafe {

        @Test
        void should_throw_an_exception() {
            assertThatThrownBy(() ->
                    cut.validateContentIsSafe(
                        "<script src=\"/external.jpg\" /><div onClick=\\\"alert('test');\\\" style=\\\"margin: auto\\\">onclick alert<div>\""
                    )
                )
                .isInstanceOf(PageContentUnsafeException.class);
        }

        @Test
        void should_not_throw_an_exception() {
            assertDoesNotThrow(() -> cut.validateContentIsSafe("content"));
        }
    }

    @Nested
    class ValidateTemplate {

        @Test
        void should_not_throw_an_exception() {
            assertDoesNotThrow(() -> cut.validateTemplate("content", API_ID, ORGANIZATION_ID));
        }
    }

    @Nested
    class ValidateAndSanitizeForCreation {

        @ParameterizedTest
        @NullSource
        @EmptySource
        void should_throw_an_error_when_name_is_not_valid(String name) {
            assertThatThrownBy(() -> cut.validateAndSanitizeForCreation(Page.builder().name(name).build(), ORGANIZATION_ID))
                .isInstanceOf(InvalidPageNameException.class);
        }

        @Test
        void should_set_content_from_source() {
            var page = Page
                .builder()
                .name("new-page")
                .type(Page.Type.MARKDOWN)
                .source(PageSource.builder().type("http").build())
                .content("")
                .referenceId(API_ID)
                .referenceType(Page.ReferenceType.API)
                .build();

            var sanitized = cut.validateAndSanitizeForCreation(page, ORGANIZATION_ID);

            assertThat(sanitized.getContent()).isEqualTo(PageSourceDomainServiceInMemory.MARKDOWN);
        }

        @Test
        void should_throw_error_if_markdown_content_is_unsafe() {
            assertThatThrownBy(() ->
                    cut.validateAndSanitizeForCreation(
                        Page.builder().type(Page.Type.MARKDOWN).name("new page").content(getNotSafe()).build(),
                        ORGANIZATION_ID
                    )
                )
                .isInstanceOf(PageContentUnsafeException.class);
        }

        @Test
        void should_throw_error_if_swagger_content_is_unsafe() {
            assertThatThrownBy(() ->
                    cut.validateAndSanitizeForCreation(
                        Page.builder().type(Page.Type.SWAGGER).name("new page").content(getNotSafe()).build(),
                        ORGANIZATION_ID
                    )
                )
                .isInstanceOf(InvalidPageContentException.class);
        }

        @Test
        void should_throw_error_if_parent_is_not_a_folder() {
            var parentId = "parent-id";
            pageCrudService.initWith(List.of(Page.builder().id(parentId).type(Page.Type.MARKDOWN).build()));

            assertThatThrownBy(() ->
                    cut.validateAndSanitizeForCreation(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page")
                            .content("")
                            .parentId(parentId)
                            .referenceId(API_ID)
                            .referenceType(Page.ReferenceType.API)
                            .build(),
                        ORGANIZATION_ID
                    )
                )
                .isInstanceOf(InvalidPageParentException.class);
        }

        @Test
        void should_throw_error_when_name_is_not_unique() {
            pageQueryService.initWith(
                List.of(
                    Page
                        .builder()
                        .name("new page")
                        .type(Page.Type.MARKDOWN)
                        .referenceId("api-id")
                        .referenceType(Page.ReferenceType.API)
                        .build()
                )
            );

            assertThatThrownBy(() ->
                    cut.validateAndSanitizeForCreation(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page")
                            .content("")
                            .referenceId("api-id")
                            .referenceType(Page.ReferenceType.API)
                            .build(),
                        ORGANIZATION_ID
                    )
                )
                .isInstanceOf(ValidationDomainException.class);
        }

        private String getNotSafe() {
            String html = "";
            html += "<script src=\"/external.jpg\" />";
            html += "<div onClick=\"alert('test');\" style=\"margin: auto\">onclick alert<div>";

            return html;
        }
    }
}
