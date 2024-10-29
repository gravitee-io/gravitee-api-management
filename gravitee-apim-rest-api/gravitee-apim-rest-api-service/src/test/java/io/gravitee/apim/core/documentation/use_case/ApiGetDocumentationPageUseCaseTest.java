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

import fixtures.core.model.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiGetDocumentationPageUseCaseTest {

    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private ApiGetDocumentationPageUseCase useCase;
    private static final String API_ID = "api-id";
    private static final String PAGE_ID = "page-id";

    @BeforeEach
    void setUp() {
        ApiDocumentationDomainService apiDocumentationDomainService = new ApiDocumentationDomainService(pageQueryService, planQueryService);
        useCase = new ApiGetDocumentationPageUseCase(apiDocumentationDomainService, apiCrudService, pageCrudService);
    }

    @AfterEach
    void tearDown() {
        pageCrudService.reset();
        pageQueryService.reset();
        apiCrudService.reset();
        planQueryService.reset();
    }

    @Test
    void should_return_page() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(Page.builder().id(PAGE_ID).referenceType(Page.ReferenceType.API).referenceId(API_ID).type(Page.Type.MARKDOWN).build())
        );
        var res = useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)).page();
        assertThat(res)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", PAGE_ID)
            .hasFieldOrPropertyWithValue("hidden", null)
            .hasFieldOrPropertyWithValue("generalConditions", false);
    }

    @Test
    void should_return_page_used_as_general_conditions() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(Page.builder().id(PAGE_ID).referenceType(Page.ReferenceType.API).referenceId(API_ID).type(Page.Type.MARKDOWN).build())
        );

        planQueryService.initWith(
            List.of(
                PlanFixtures
                    .aPlanHttpV4()
                    .toBuilder()
                    .id("plan-1")
                    .apiId(API_ID)
                    .generalConditions(PAGE_ID)
                    .build()
                    .setPlanStatus(PlanStatus.PUBLISHED)
            )
        );

        var res = useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)).page();
        assertThat(res)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", PAGE_ID)
            .hasFieldOrPropertyWithValue("hidden", null)
            .hasFieldOrPropertyWithValue("generalConditions", true);
    }

    @Test
    void should_return_folder_without_children() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(Page.builder().id(PAGE_ID).referenceType(Page.ReferenceType.API).referenceId(API_ID).type(Page.Type.FOLDER).build())
        );
        var res = useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)).page();
        assertThat(res)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", PAGE_ID)
            .hasFieldOrPropertyWithValue("hidden", true)
            .hasFieldOrPropertyWithValue("generalConditions", null);
    }

    @Test
    void should_return_folder_with_published_children() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder().id(PAGE_ID).referenceType(Page.ReferenceType.API).referenceId(API_ID).type(Page.Type.FOLDER).build(),
                Page.builder().id("one-child").parentId(PAGE_ID).published(true).build()
            )
        );
        var res = useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)).page();
        assertThat(res).isNotNull().hasFieldOrPropertyWithValue("id", PAGE_ID).hasFieldOrPropertyWithValue("hidden", false);
    }

    @Test
    void should_return_folder_with_unpublished_children() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder().id(PAGE_ID).referenceType(Page.ReferenceType.API).referenceId(API_ID).type(Page.Type.FOLDER).build(),
                Page.builder().id("one-child").parentId(PAGE_ID).published(false).build()
            )
        );
        var res = useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)).page();
        assertThat(res).isNotNull().hasFieldOrPropertyWithValue("id", PAGE_ID).hasFieldOrPropertyWithValue("hidden", true);
    }

    @Test
    void should_throw_error_if_api_does_not_exist() {
        initPageServices(
            List.of(Page.builder().id("page#1").referenceType(Page.ReferenceType.API).referenceId(API_ID).type(Page.Type.MARKDOWN).build())
        );
        assertThatThrownBy(() -> useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)))
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_error_if_page_not_associated_to_api_by_id() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page
                    .builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID + "-bad")
                    .type(Page.Type.MARKDOWN)
                    .build()
            )
        );
        assertThatThrownBy(() -> useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)))
            .isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void should_throw_error_if_page_not_associated_to_api_by_reference_type() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page
                    .builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.ENVIRONMENT)
                    .referenceId(API_ID)
                    .type(Page.Type.MARKDOWN)
                    .build()
            )
        );
        assertThatThrownBy(() -> useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)))
            .isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void should_throw_error_if_page_does_not_exist() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(List.of());
        assertThatThrownBy(() -> useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, PAGE_ID)))
            .isInstanceOf(PageNotFoundException.class);
    }

    @Test
    void should_throw_error_if_page_id_is_root() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        assertThatThrownBy(() -> useCase.execute(new ApiGetDocumentationPageUseCase.Input(API_ID, "ROOT")))
            .isInstanceOf(PageNotFoundException.class);
    }

    private void initPageServices(List<Page> pages) {
        pageCrudService.initWith(pages);
        pageQueryService.initWith(pages);
    }

    private void initApiServices(List<Api> apis) {
        apiCrudService.initWith(apis);
    }
}
