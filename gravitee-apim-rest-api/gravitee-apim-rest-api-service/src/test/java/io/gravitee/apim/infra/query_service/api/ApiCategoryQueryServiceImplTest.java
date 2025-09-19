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
package io.gravitee.apim.infra.query_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiCategoryQueryServiceImplTest {

    private static final String CATEGORY_ID_2 = "category2";
    private static final String CATEGORY_ID_1 = "category1";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final Api API = ApiFixtures.aProxyApiV4()
        .toBuilder()
        .environmentId(ENVIRONMENT_ID)
        .categories(Set.of(CATEGORY_ID_1, CATEGORY_ID_2))
        .build();

    @Mock
    CategoryRepository categoryRepository;

    ApiCategoryQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiCategoryQueryServiceImpl(categoryRepository);
    }

    @Nested
    class FindApiCategoryKeys {

        @Test
        @SneakyThrows
        void should_query_only_categories_of_given_api() {
            // when
            service.findApiCategoryKeys(API);

            // then
            verify(categoryRepository).findAllByEnvironment(ENVIRONMENT_ID);
        }

        @Test
        @SneakyThrows
        void should_return_category_keys() {
            // given
            // Uncomment when https://gravitee.atlassian.net/browse/APIM-4437 is fixed
            //            when(categoryRepository.findByEnvironmentIdAndIdIn(any(), any()))
            //                .thenAnswer(invocation -> {
            //                    Set<String> categoryIds = invocation.getArgument(1);
            //
            //                    return categoryIds
            //                        .stream()
            //                        .map(categoryId -> Category.builder().id(categoryId).key("key-" + categoryId).build())
            //                        .collect(Collectors.toSet());
            //                });

            when(categoryRepository.findAllByEnvironment(ENVIRONMENT_ID)).thenAnswer(invocation -> {
                return Set.of(
                    Category.builder().id(CATEGORY_ID_1).key("key-" + CATEGORY_ID_1).build(),
                    Category.builder().id(CATEGORY_ID_2).key("key-" + CATEGORY_ID_2).build()
                );
            });

            // when
            var categories = service.findApiCategoryKeys(API);

            // then
            assertThat(categories).containsExactlyInAnyOrder("key-" + CATEGORY_ID_1, "key-" + CATEGORY_ID_2);
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @SneakyThrows
        void should_return_empty_list_when_api_has_no_category_defined(Set<String> categoryIds) {
            // when
            var categories = service.findApiCategoryKeys(API.toBuilder().categories(categoryIds).build());

            // then
            assertThat(categories).isEmpty();
            verifyNoInteractions(categoryRepository);
        }

        @Test
        @SneakyThrows
        void should_throw_when_fail_to_fetch_categories() {
            // given
            when(categoryRepository.findAllByEnvironment(any())).thenThrow(new TechnicalException("error"));

            Throwable throwable = catchThrowable(() -> service.findApiCategoryKeys(API));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessageContaining("An error occurs while trying to find Categories for the api: " + API.getId())
                .hasCauseInstanceOf(TechnicalException.class)
                .hasRootCauseMessage("error");
        }
    }

    @Nested
    class FindByEnvironmentId {

        @Test
        @SneakyThrows
        void should_return_categories() {
            when(categoryRepository.findAllByEnvironment(ENVIRONMENT_ID)).thenReturn(
                Set.of(
                    Category.builder().id(CATEGORY_ID_1).key("key-" + CATEGORY_ID_1).build(),
                    Category.builder().id(CATEGORY_ID_2).key("key-" + CATEGORY_ID_2).build()
                )
            );

            assertThat(service.findByEnvironmentId(ENVIRONMENT_ID))
                .extracting("id")
                .containsExactlyInAnyOrder(CATEGORY_ID_1, CATEGORY_ID_2);
        }

        @Test
        @SneakyThrows
        void should_throw_when_failing_to_fetch_categories() {
            when(categoryRepository.findAllByEnvironment(any())).thenThrow(new TechnicalException("error"));

            assertThat(catchThrowable(() -> service.findByEnvironmentId(ENVIRONMENT_ID)))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessageContaining("Cannot find categories for environment environment-id")
                .hasCauseInstanceOf(TechnicalException.class)
                .hasRootCauseMessage("error");
        }
    }
}
