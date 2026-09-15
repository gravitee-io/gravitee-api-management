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
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        apiQueryServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi()));

        var input = new GetIngestedApisUseCase.Input(INTEGRATION_ID);

        var ingestedApis = usecase.execute(input).ingestedApis();

        AssertionsForClassTypes.assertThat(ingestedApis)
            .extracting(Page::getPageNumber, Page::getPageElements, Page::getTotalElements)
            .containsExactly(0, 1L, 1L);
    }

    @ParameterizedTest
    @MethodSource
    void should_return_an_empty_page_when_the_requested_page_starts_past_the_last_match(
        List<Api> ingestedApis,
        Pageable pageable,
        int expectedPageNumber,
        long expectedTotalElements
    ) {
        apiQueryServiceInMemory.initWith(ingestedApis);

        var input = new GetIngestedApisUseCase.Input(INTEGRATION_ID, pageable);

        var output = usecase.execute(input).ingestedApis();

        assertThat(output.getContent()).isEmpty();
        AssertionsForClassTypes.assertThat(output)
            .extracting(Page::getPageNumber, Page::getTotalElements)
            .containsExactly(expectedPageNumber, expectedTotalElements);
    }

    private static Stream<Arguments> should_return_an_empty_page_when_the_requested_page_starts_past_the_last_match() {
        return Stream.of(
            Arguments.of(federatedApis("api-1"), new PageableImpl(2, 10), 1, 1L),
            Arguments.of(federatedApis("api-1", "api-2", "api-3"), new PageableImpl(3, 2), 2, 3L)
        );
    }

    private static List<Api> federatedApis(String... ids) {
        return Stream.of(ids)
            .<Api>map(id -> ApiFixtures.aFederatedApi().toBuilder().id(id).build())
            .toList();
    }
}
