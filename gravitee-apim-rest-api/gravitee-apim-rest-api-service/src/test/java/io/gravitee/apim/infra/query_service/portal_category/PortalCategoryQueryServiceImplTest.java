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
package io.gravitee.apim.infra.query_service.portal_category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalCategoryFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.PortalCategoryAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalCategoryRepository;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortalCategoryQueryServiceImplTest {

    PortalCategoryRepository repository;
    PortalCategoryQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PortalCategoryRepository.class);
        service = new PortalCategoryQueryServiceImpl(repository);
    }

    @Nested
    class FindByEnvironmentId {

        @Test
        @SneakyThrows
        void should_return_categories_adapted_from_the_repository() {
            var category = PortalCategoryFixtures.aPortalCategory();
            when(repository.findAllByEnvironmentId("env")).thenReturn(List.of(PortalCategoryAdapter.INSTANCE.toRepository(category)));

            var result = service.findByEnvironmentId("env");

            assertThat(result).containsExactly(category);
        }

        @Test
        @SneakyThrows
        void should_return_empty_list_when_none_found() {
            when(repository.findAllByEnvironmentId("env")).thenReturn(List.of());

            assertThat(service.findByEnvironmentId("env")).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            when(repository.findAllByEnvironmentId(any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.findByEnvironmentId("env"));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to find portal categories for environment: env");
        }
    }
}
