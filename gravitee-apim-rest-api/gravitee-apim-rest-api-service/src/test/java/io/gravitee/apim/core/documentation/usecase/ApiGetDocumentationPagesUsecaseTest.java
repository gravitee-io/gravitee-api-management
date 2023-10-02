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
package io.gravitee.apim.core.documentation.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PageQueryServiceInMemory;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ApiGetDocumentationPagesUsecaseTest {

    private final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    private final ApiGetDocumentationPagesUsecase useCase = new ApiGetDocumentationPagesUsecase(
        new ApiDocumentationDomainService(pageQueryService)
    );

    @AfterEach
    void tearDown() {
        pageQueryService.reset();
    }

    @Test
    void should_return_pages_for_api() {
        pageQueryService.initWith(
            List.of(
                Page.builder().id("page#1").referenceType(Page.ReferenceType.API).referenceId("api-id").build(),
                Page.builder().id("page#2").referenceType(Page.ReferenceType.API).referenceId("api-id").build()
            )
        );
        var res = useCase.execute(new ApiGetDocumentationPagesUsecase.Input("api-id")).pages();
        assertThat(res).hasSize(2);
        assertThat(res.get(0).getId()).isEqualTo("page#1");
        assertThat(res.get(1).getId()).isEqualTo("page#2");
    }
}
