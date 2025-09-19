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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PageQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiDocumentationDomainServiceTest {

    private final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final ApiDocumentationDomainService service = new ApiDocumentationDomainService(pageQueryService, planQueryService);

    @AfterEach
    void tearDown() {
        pageQueryService.reset();
    }

    @Nested
    class GetAllPages {

        @Test
        void should_return_all_pages_with_null_parent_id() {
            pageQueryService.initWith(
                List.of(
                    Page.builder().id("page#1").referenceType(Page.ReferenceType.API).referenceId("api-id").build(),
                    Page.builder().id("page#2").referenceType(Page.ReferenceType.API).referenceId("api-id").build(),
                    Page.builder().id("page#3").referenceType(Page.ReferenceType.API).referenceId("api-id").parentId("not-root").build()
                )
            );
            var res = service.getApiPages("api-id", null);
            assertThat(res).hasSize(3);
            assertThat(res.get(0).getId()).isEqualTo("page#1");
            assertThat(res.get(1).getId()).isEqualTo("page#2");
            assertThat(res.get(2).getId()).isEqualTo("page#3");
        }

        @Test
        void should_return_all_pages_with_empty_parent_id() {
            pageQueryService.initWith(
                List.of(
                    Page.builder().id("page#1").referenceType(Page.ReferenceType.API).referenceId("api-id").build(),
                    Page.builder().id("page#2").referenceType(Page.ReferenceType.API).referenceId("api-id").build(),
                    Page.builder().id("page#3").referenceType(Page.ReferenceType.API).referenceId("api-id").parentId("not-root").build()
                )
            );
            var res = service.getApiPages("api-id", "");
            assertThat(res).hasSize(3);
            assertThat(res.get(0).getId()).isEqualTo("page#1");
            assertThat(res.get(1).getId()).isEqualTo("page#2");
            assertThat(res.get(2).getId()).isEqualTo("page#3");
        }
    }

    @Nested
    class GetRootPages {

        @Test
        void should_return_all_root_pages() {
            pageQueryService.initWith(
                List.of(
                    Page.builder().id("page#1").referenceType(Page.ReferenceType.API).referenceId("api-id").build(),
                    Page.builder().id("page#2").referenceType(Page.ReferenceType.API).referenceId("api-id").parentId("not-root").build(),
                    Page.builder().id("page#3").referenceType(Page.ReferenceType.API).referenceId("api-id").parentId("").build()
                )
            );
            var res = service.getApiPages("api-id", "ROOT");
            assertThat(res).hasSize(2);
            assertThat(res.get(0).getId()).isEqualTo("page#1");
            assertThat(res.get(1).getId()).isEqualTo("page#3");
        }
    }

    @Nested
    class GetPageByParentId {

        @Test
        void should_return_all_pages_with_parent_id() {
            pageQueryService.initWith(
                List.of(
                    Page.builder().id("page#1").referenceType(Page.ReferenceType.API).referenceId("api-id").build(),
                    Page.builder().id("page#2").referenceType(Page.ReferenceType.API).referenceId("api-id").parentId("not-root").build(),
                    Page.builder().id("page#3").referenceType(Page.ReferenceType.API).referenceId("api-id").parentId("parent-id").build(),
                    Page.builder()
                        .id("page#4")
                        .referenceType(Page.ReferenceType.API)
                        .referenceId("bad-api-id")
                        .parentId("parent-id")
                        .build(),
                    Page.builder().id("page#5").referenceType(Page.ReferenceType.API).referenceId("api-id").parentId("").build()
                )
            );
            var res = service.getApiPages("api-id", "parent-id");
            assertThat(res).hasSize(1);
            assertThat(res.get(0).getId()).isEqualTo("page#3");
        }
    }
}
