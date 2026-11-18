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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiPathMappingsDomainService;
import io.gravitee.apim.core.api.domain_service.OpenApiPathMappingsExtractor;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.HttpListenerPathMappings;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportApiPathMappingsUseCaseTest {

    private static final String API_ID = "api-1";
    private static final String PAGE_ID = "page-1";
    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2026-03-25T10:00:00Z");

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    private final PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
    private final PortalPageContentQueryServiceInMemory portalPageContentQueryService = new PortalPageContentQueryServiceInMemory();

    private ImportApiPathMappingsUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.of("UTC")));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var domainService = new ApiPathMappingsDomainService(apiCrudService, auditDomainService);
        OpenApiPathMappingsExtractor extractor = content -> List.of("/pets/:petId", "/pets");
        useCase = new ImportApiPathMappingsUseCase(
            domainService,
            pageCrudService,
            extractor,
            navigationItemsQueryService,
            portalPageContentQueryService
        );

        var api = withMappings(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).environmentId(ENV_ID).build(), Set.of("/existing"));
        apiCrudService.initWith(List.of(api));
        pageCrudService.initWith(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.SWAGGER)
                    .content("openapi: 3.0.0")
                    .build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        apiCrudService.reset();
        pageCrudService.reset();
        auditCrudService.reset();
        userCrudService.reset();
        navigationItemsQueryService.reset();
        portalPageContentQueryService.reset();
    }

    @Test
    void should_append_and_dedupe_when_overwrite_is_false() {
        var output = useCase.execute(input(false));
        assertThat(output.pathMappings()).containsExactly("/existing", "/pets/:petId", "/pets");
    }

    @Test
    void should_replace_when_overwrite_is_true() {
        var output = useCase.execute(input(true));
        assertThat(output.pathMappings()).containsExactly("/pets/:petId", "/pets");
    }

    @Test
    void should_return_not_found_when_page_belongs_to_another_api() {
        pageCrudService.reset();
        pageCrudService.initWith(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId("other-api")
                    .type(Page.Type.SWAGGER)
                    .content("openapi: 3.0.0")
                    .build()
            )
        );
        assertThatThrownBy(() -> useCase.execute(input(false))).isInstanceOf(PageNotFoundException.class);
    }

    @Test
    void should_reject_non_swagger_page() {
        pageCrudService.reset();
        pageCrudService.initWith(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.MARKDOWN)
                    .content("# doc")
                    .build()
            )
        );
        assertThatThrownBy(() -> useCase.execute(input(false))).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void should_return_not_found_when_api_is_in_another_environment() {
        apiCrudService.reset();
        var api = withMappings(
            ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).environmentId("other-environment").build(),
            Set.of("/existing")
        );
        apiCrudService.initWith(List.of(api));

        assertThatThrownBy(() -> useCase.execute(input(false))).isInstanceOf(ApiNotFoundException.class);
        assertThat(
            HttpListenerPathMappings.firstHttpListenerMappings(apiCrudService.storage().getFirst().getApiDefinitionHttpV4().getListeners())
        ).hasValue(Set.of("/existing"));
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_kubernetes_api() {
        apiCrudService.reset();
        var api = withMappings(
            ApiFixtures.aProxyApiV4()
                .toBuilder()
                .id(API_ID)
                .environmentId(ENV_ID)
                .originContext(new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED))
                .build(),
            Set.of("/existing")
        );
        apiCrudService.initWith(List.of(api));
        assertThatThrownBy(() -> useCase.execute(input(false))).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void should_return_not_found_when_page_belongs_to_the_environment() {
        pageCrudService.reset();
        pageCrudService.initWith(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.ENVIRONMENT)
                    .referenceId(ENV_ID)
                    .type(Page.Type.SWAGGER)
                    .content("openapi: 3.0.0")
                    .build()
            )
        );

        assertThatThrownBy(() -> useCase.execute(input(true))).isInstanceOf(PageNotFoundException.class);
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_return_not_found_when_a_non_swagger_page_belongs_to_another_api() {
        pageCrudService.reset();
        pageCrudService.initWith(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId("other-api")
                    .type(Page.Type.MARKDOWN)
                    .content("# doc")
                    .build()
            )
        );

        assertThatThrownBy(() -> useCase.execute(input(false))).isInstanceOf(PageNotFoundException.class);
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_import_a_next_gen_openapi_page_of_this_api() {
        pageCrudService.reset();
        var contentId = PortalPageContentId.random();
        var navId = PortalNavigationItemId.random();
        portalPageContentQueryService.initWith(
            List.of(
                new OpenApiPageContent(
                    contentId,
                    "org-1",
                    ENV_ID,
                    OpenApi.of("openapi: 3.0.0"),
                    new io.gravitee.apim.core.portal_page.model.RedocConfiguration("https://try.example")
                )
            )
        );
        navigationItemsQueryService.initWith(
            List.of(
                PortalNavigationPage.builder()
                    .id(navId)
                    .organizationId("org-1")
                    .environmentId(ENV_ID)
                    .reference(new NavigationItemReference.ApiReference(API_ID))
                    .title("Petstore")
                    .segment("petstore")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .portalPageContentId(contentId)
                    .published(true)
                    .visibility(PortalVisibility.PUBLIC)
                    .build()
            )
        );

        var output = useCase.execute(
            new ImportApiPathMappingsUseCase.Input(
                API_ID,
                navId.json(),
                true,
                AuditInfo.builder()
                    .organizationId("org-1")
                    .environmentId(ENV_ID)
                    .actor(AuditActor.builder().userId("user-1").build())
                    .build()
            )
        );

        assertThat(output.pathMappings()).containsExactly("/pets/:petId", "/pets");
    }

    @Test
    void should_return_not_found_when_classic_portal_page_belongs_to_another_environment() {
        pageCrudService.reset();
        pageCrudService.initWith(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.ENVIRONMENT)
                    .referenceId("other-environment")
                    .type(Page.Type.SWAGGER)
                    .content("openapi: 3.0.0")
                    .build()
            )
        );

        assertThatThrownBy(() -> useCase.execute(input(false))).isInstanceOf(PageNotFoundException.class);
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_import_an_unpublished_swagger_page_of_this_api() {
        pageCrudService.reset();
        pageCrudService.initWith(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.SWAGGER)
                    .published(false)
                    .content("openapi: 3.0.0")
                    .build()
            )
        );

        var output = useCase.execute(input(true));

        assertThat(output.pathMappings()).containsExactly("/pets/:petId", "/pets");
    }

    @Test
    void should_return_not_found_when_next_gen_page_id_is_not_a_uuid() {
        assertThatThrownBy(() -> useCase.execute(input("not-a-uuid", false))).isInstanceOf(PageNotFoundException.class);
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_return_not_found_when_next_gen_page_is_missing() {
        pageCrudService.reset();

        assertThatThrownBy(() -> useCase.execute(input(PortalNavigationItemId.random().json(), false))).isInstanceOf(
            PageNotFoundException.class
        );
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_return_not_found_when_next_gen_page_belongs_to_another_api() {
        var pageId = givenNextGenOpenApiPage("other-api", ENV_ID, true);

        assertThatThrownBy(() -> useCase.execute(input(pageId, false))).isInstanceOf(PageNotFoundException.class);
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_return_not_found_when_next_gen_item_is_not_a_page() {
        pageCrudService.reset();
        var navId = PortalNavigationItemId.random();
        navigationItemsQueryService.initWith(
            List.of(
                PortalNavigationFolder.builder()
                    .id(navId)
                    .organizationId("org-1")
                    .environmentId(ENV_ID)
                    .reference(new NavigationItemReference.ApiReference(API_ID))
                    .title("Folder")
                    .segment("folder")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .published(true)
                    .visibility(PortalVisibility.PUBLIC)
                    .build()
            )
        );

        assertThatThrownBy(() -> useCase.execute(input(navId.json(), false))).isInstanceOf(PageNotFoundException.class);
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_return_not_found_when_next_gen_content_is_missing() {
        pageCrudService.reset();
        var navId = PortalNavigationItemId.random();
        navigationItemsQueryService.initWith(List.of(navigationPage(navId, API_ID, PortalPageContentId.random(), true)));

        assertThatThrownBy(() -> useCase.execute(input(navId.json(), false))).isInstanceOf(PageNotFoundException.class);
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_return_not_found_when_next_gen_content_belongs_to_another_environment() {
        var pageId = givenNextGenOpenApiPage(API_ID, "other-environment", true);

        assertThatThrownBy(() -> useCase.execute(input(pageId, false))).isInstanceOf(PageNotFoundException.class);
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_reject_next_gen_content_that_is_not_openapi() {
        pageCrudService.reset();
        var contentId = PortalPageContentId.random();
        var navId = PortalNavigationItemId.random();
        portalPageContentQueryService.initWith(
            List.of(new GraviteeMarkdownPageContent(contentId, "org-1", ENV_ID, GraviteeMarkdown.of("# doc")))
        );
        navigationItemsQueryService.initWith(List.of(navigationPage(navId, API_ID, contentId, true)));

        assertThatThrownBy(() -> useCase.execute(input(navId.json(), false)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Page must be an OpenAPI document");
        assertThat(storedMappings()).containsExactly("/existing");
    }

    @Test
    void should_import_an_unpublished_next_gen_openapi_page() {
        var pageId = givenNextGenOpenApiPage(API_ID, ENV_ID, false);

        var output = useCase.execute(input(pageId, true));

        assertThat(output.pathMappings()).containsExactly("/pets/:petId", "/pets");
    }

    @Test
    void should_create_audit_log() {
        useCase.execute(input(true));
        assertThat(auditCrudService.storage()).isNotEmpty();
    }

    @Test
    void should_record_when_the_api_was_updated() {
        useCase.execute(input(true));
        assertThat(apiCrudService.storage().getFirst().getUpdatedAt()).isEqualTo(TimeProvider.now());
    }

    private ImportApiPathMappingsUseCase.Input input(boolean overwrite) {
        return input(PAGE_ID, overwrite);
    }

    private ImportApiPathMappingsUseCase.Input input(String pageId, boolean overwrite) {
        return new ImportApiPathMappingsUseCase.Input(
            API_ID,
            pageId,
            overwrite,
            AuditInfo.builder().organizationId("org-1").environmentId(ENV_ID).actor(AuditActor.builder().userId("user-1").build()).build()
        );
    }

    private String givenNextGenOpenApiPage(String apiId, String contentEnvironmentId, boolean published) {
        pageCrudService.reset();
        var contentId = PortalPageContentId.random();
        var navId = PortalNavigationItemId.random();
        portalPageContentQueryService.initWith(
            List.of(
                new OpenApiPageContent(
                    contentId,
                    "org-1",
                    contentEnvironmentId,
                    OpenApi.of("openapi: 3.0.0"),
                    new RedocConfiguration("https://try.example")
                )
            )
        );
        navigationItemsQueryService.initWith(List.of(navigationPage(navId, apiId, contentId, published)));
        return navId.json();
    }

    private static PortalNavigationPage navigationPage(
        PortalNavigationItemId navId,
        String apiId,
        PortalPageContentId contentId,
        boolean published
    ) {
        return PortalNavigationPage.builder()
            .id(navId)
            .organizationId("org-1")
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.ApiReference(apiId))
            .title("Petstore")
            .segment("petstore")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(contentId)
            .published(published)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private Set<String> storedMappings() {
        return HttpListenerPathMappings.firstHttpListenerMappings(
            apiCrudService.storage().getFirst().getApiDefinitionHttpV4().getListeners()
        ).orElseThrow();
    }

    private static io.gravitee.apim.core.api.model.Api withMappings(io.gravitee.apim.core.api.model.Api api, Set<String> mappings) {
        var definition = api.getApiDefinitionHttpV4();
        var listeners = HttpListenerPathMappings.withFirstHttpListenerMappings(definition.getListeners(), new LinkedHashSet<>(mappings));
        return api.toBuilder().apiDefinitionValue(definition.toBuilder().listeners(listeners).build()).build();
    }
}
