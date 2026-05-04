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
package io.gravitee.gamma.core.resource.use_case;

import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.anAuditInfo;
import static org.assertj.core.api.Assertions.assertThat;

import gamma.inmemory.ResourceCrudServiceInMemory;
import gamma.inmemory.ResourceQueryServiceInMemory;
import io.gravitee.gamma.core.domain.resource.use_case.SearchResourceUseCase;
import io.gravitee.gamma.core.resource.fixture.ResourceFixture;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchResourceUseCaseTest {

    private ResourceCrudServiceInMemory crud;
    private SearchResourceUseCase useCase;

    @BeforeEach
    void setUp() {
        crud = new ResourceCrudServiceInMemory();
        useCase = new SearchResourceUseCase(new ResourceQueryServiceInMemory(crud));
    }

    @Test
    void should_paginate_results_for_current_environment() {
        crud.initWith(
            List.of(
                ResourceFixture.aResource(r -> r.id("1").definition(ResourceFixture.aDefinition(d -> d.name("alpha")))),
                ResourceFixture.aResource(r -> r.id("2").definition(ResourceFixture.aDefinition(d -> d.name("beta")))),
                ResourceFixture.aResource(r -> r.id("3").referenceId("OTHER").definition(ResourceFixture.aDefinition(d -> d.name("gamma"))))
            )
        );

        var output = useCase.execute(new SearchResourceUseCase.Input(anAuditInfo(), pageable(1, 10), null));

        assertThat(output.resources().getContent()).hasSize(2);
        assertThat(output.resources().getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_filter_by_query_matching_name_or_type() {
        crud.initWith(
            List.of(
                ResourceFixture.aResource(r -> r.id("1").definition(ResourceFixture.aDefinition(d -> d.name("alpha-cache").type("cache")))),
                ResourceFixture.aResource(r -> r.id("2").definition(ResourceFixture.aDefinition(d -> d.name("beta-oauth").type("oauth2"))))
            )
        );

        var output = useCase.execute(new SearchResourceUseCase.Input(anAuditInfo(), pageable(1, 10), "oauth"));

        assertThat(output.resources().getContent()).hasSize(1);
        assertThat(output.resources().getContent().get(0).definition().getName()).isEqualTo("beta-oauth");
    }

    private static Pageable pageable(int page, int size) {
        return new PageableImpl(page, size);
    }
}
