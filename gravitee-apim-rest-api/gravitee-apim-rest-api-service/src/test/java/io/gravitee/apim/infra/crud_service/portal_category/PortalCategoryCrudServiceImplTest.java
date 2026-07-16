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
package io.gravitee.apim.infra.crud_service.portal_category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalCategoryFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.PortalCategoryAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalCategoryRepository;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortalCategoryCrudServiceImplTest {

    PortalCategoryRepository repository;
    PortalCategoryCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PortalCategoryRepository.class);
        service = new PortalCategoryCrudServiceImpl(repository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_portal_category() {
            var category = PortalCategoryFixtures.aPortalCategory();
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.create(category);

            assertThat(result).isEqualTo(category);
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            var category = PortalCategoryFixtures.aPortalCategory();
            when(repository.create(any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.create(category));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to create a portal category");
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_and_return_the_updated_portal_category() {
            var category = PortalCategoryFixtures.aPortalCategory();
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.update(category);

            assertThat(result).isEqualTo(category);
            verify(repository).update(PortalCategoryAdapter.INSTANCE.toRepository(category));
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            var category = PortalCategoryFixtures.aPortalCategory();
            when(repository.update(any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.update(category));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to update portal category: " + category.getId());
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_portal_category() throws TechnicalException {
            var id = PortalCategoryFixtures.PORTAL_CATEGORY_ID;

            service.delete(id);

            verify(repository).delete(id.toString());
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            var id = PortalCategoryFixtures.PORTAL_CATEGORY_ID;
            doThrow(new TechnicalException("boom")).when(repository).delete(id.toString());

            assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to delete portal category: " + id);
        }
    }

    @Nested
    class Get {

        @Test
        @SneakyThrows
        void should_return_portal_category_and_adapt_it() {
            var id = PortalCategoryFixtures.PORTAL_CATEGORY_ID;
            var category = PortalCategoryFixtures.aPortalCategory();
            when(repository.findById(id.toString())).thenReturn(Optional.of(PortalCategoryAdapter.INSTANCE.toRepository(category)));

            var result = service.get(id);

            assertThat(result).contains(category);
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_not_found() {
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThat(service.get(PortalCategoryFixtures.PORTAL_CATEGORY_ID)).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            var id = PortalCategoryFixtures.PORTAL_CATEGORY_ID;
            when(repository.findById(any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.get(id));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to find portal category: " + id);
        }
    }
}
