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
package io.gravitee.apim.core.integration.use_case;

import static assertions.CoreAssertions.assertThat;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.common.data.domain.Page;
import java.util.List;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetIngestedApisUseCaseTest {

    private final String INTEGRATION_ID = "integration-id";

    ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory();
    GetIngestedApisUseCase usecase;

    @BeforeEach
    void setUp() {
        usecase = new GetIngestedApisUseCase(apiQueryServiceInMemory);
    }

    @Test
    void should_return_ingested_apis_list() {
        apiQueryServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi()));

        var input = new GetIngestedApisUseCase.Input(INTEGRATION_ID);

        var output = usecase.execute(input).ingestedApis();

        assertThat(output).isNotNull();
        assertThat(output.getContent()).hasSize(1).extracting(Api::getId, Api::getName).containsExactly(Tuple.tuple("my-api", "My Api"));
    }

    @Test
    void should_return_page_with_default_pageable() {
        var apis = List.of(ApiFixtures.aFederatedApi());
        apiQueryServiceInMemory.initWith(apis);

        var input = new GetIngestedApisUseCase.Input(INTEGRATION_ID);

        var ingestedApis = usecase.execute(input).ingestedApis();

        AssertionsForClassTypes.assertThat(ingestedApis).isNotNull();
        AssertionsForClassTypes.assertThat(ingestedApis)
            .extracting(Page::getContent, Page::getPageNumber, Page::getPageElements, Page::getTotalElements)
            .containsExactly(apis, 1, ingestedApis.getPageElements(), (long) ingestedApis.getContent().size());
    }
}
