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
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiPathMappingsDomainService;
import io.gravitee.apim.core.api.domain_service.OpenApiPathMappingsExtractor;
import io.gravitee.apim.core.api.model.HttpListenerPathMappings;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
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
        useCase = new ImportApiPathMappingsUseCase(domainService, pageCrudService, extractor);

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
    void should_create_audit_log() {
        useCase.execute(input(true));
        assertThat(auditCrudService.storage()).isNotEmpty();
    }

    private ImportApiPathMappingsUseCase.Input input(boolean overwrite) {
        return new ImportApiPathMappingsUseCase.Input(
            API_ID,
            PAGE_ID,
            overwrite,
            AuditInfo.builder().organizationId("org-1").environmentId(ENV_ID).actor(AuditActor.builder().userId("user-1").build()).build()
        );
    }

    private static io.gravitee.apim.core.api.model.Api withMappings(io.gravitee.apim.core.api.model.Api api, Set<String> mappings) {
        var definition = api.getApiDefinitionHttpV4();
        var listeners = HttpListenerPathMappings.withFirstHttpListenerMappings(definition.getListeners(), new LinkedHashSet<>(mappings));
        return api.toBuilder().apiDefinitionValue(definition.toBuilder().listeners(listeners).build()).build();
    }
}
