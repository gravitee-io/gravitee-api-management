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
package io.gravitee.apim.infra.query_service.portal_menu_link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkVisibility;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalMenuLinkRepository;
import io.gravitee.repository.management.model.PortalMenuLink;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortalMenuLinkQueryServiceImplTest {

    PortalMenuLinkRepository repository;
    PortalMenuLinkQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PortalMenuLinkRepository.class);
        service = new PortalMenuLinkQueryServiceImpl(repository);
    }

    @Nested
    class FindByEnvironmentIdSortByOrder {

        @Test
        @SneakyThrows
        void findByEnvironmentId_should_return_empty_page() {
            // Given
            String environmentId = "environmentId";
            when(repository.findByEnvironmentIdSortByOrder(environmentId)).thenReturn(List.of());

            // When
            var result = service.findByEnvironmentIdSortByOrder(environmentId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @SneakyThrows
        void findByEnvironmentId_should_return_portal_menu_links() {
            // Given
            String environmentId = "environmentId";
            when(repository.findByEnvironmentIdSortByOrder(environmentId))
                .thenReturn(
                    List.of(aRepositoryPortalMenuLink("id", 1), aRepositoryPortalMenuLink("id2", 2), aRepositoryPortalMenuLink("id3", 3))
                );

            // When
            var result = service.findByEnvironmentIdSortByOrder(environmentId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getId()).isEqualTo("id");
            assertThat(result.get(0).getOrder()).isEqualTo(1);
            assertThat(result.get(1).getId()).isEqualTo("id2");
            assertThat(result.get(1).getOrder()).isEqualTo(2);
            assertThat(result.get(2).getId()).isEqualTo("id3");
            assertThat(result.get(2).getOrder()).isEqualTo(3);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            String environmentId = "environmentId";
            when(repository.findByEnvironmentIdSortByOrder(environmentId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findByEnvironmentIdSortByOrder(environmentId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while searching portal menu links by environment ID environmentId");
        }
    }

    @Nested
    class FindByEnvironmentIdAndVisibilitySortByOrder {

        @Test
        @SneakyThrows
        void findByEnvironmentIdAndVisibility_should_return_empty_page() {
            // Given
            String environmentId = "environmentId";
            when(repository.findByEnvironmentIdAndVisibilitySortByOrder(environmentId, PortalMenuLink.PortalMenuLinkVisibility.PUBLIC))
                .thenReturn(List.of());

            // When
            var result = service.findByEnvironmentIdAndVisibilitySortByOrder(environmentId, PortalMenuLinkVisibility.PUBLIC);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @SneakyThrows
        void findByEnvironmentIdAndVisibility_should_return_portal_menu_links() {
            // Given
            String environmentId = "environmentId";
            when(repository.findByEnvironmentIdAndVisibilitySortByOrder(environmentId, PortalMenuLink.PortalMenuLinkVisibility.PUBLIC))
                .thenReturn(
                    List.of(aRepositoryPortalMenuLink("id", 1), aRepositoryPortalMenuLink("id2", 2), aRepositoryPortalMenuLink("id3", 3))
                );

            // When
            var result = service.findByEnvironmentIdAndVisibilitySortByOrder(environmentId, PortalMenuLinkVisibility.PUBLIC);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getId()).isEqualTo("id");
            assertThat(result.get(0).getOrder()).isEqualTo(1);
            assertThat(result.get(0).getVisibility()).isEqualTo(PortalMenuLinkVisibility.PUBLIC);
            assertThat(result.get(1).getId()).isEqualTo("id2");
            assertThat(result.get(1).getOrder()).isEqualTo(2);
            assertThat(result.get(1).getVisibility()).isEqualTo(PortalMenuLinkVisibility.PUBLIC);
            assertThat(result.get(2).getId()).isEqualTo("id3");
            assertThat(result.get(2).getOrder()).isEqualTo(3);
            assertThat(result.get(2).getVisibility()).isEqualTo(PortalMenuLinkVisibility.PUBLIC);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            String environmentId = "environmentId";
            when(repository.findByEnvironmentIdAndVisibilitySortByOrder(environmentId, PortalMenuLink.PortalMenuLinkVisibility.PRIVATE))
                .thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() ->
                service.findByEnvironmentIdAndVisibilitySortByOrder(environmentId, PortalMenuLinkVisibility.PRIVATE)
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while searching portal menu links by environment ID environmentId and visibility PRIVATE");
        }
    }

    private io.gravitee.repository.management.model.PortalMenuLink aRepositoryPortalMenuLink(String id, int order) {
        return io.gravitee.repository.management.model.PortalMenuLink
            .builder()
            .id(id)
            .environmentId("environmentId")
            .name("name-" + order)
            .target("target-" + order)
            .type(PortalMenuLink.PortalMenuLinkType.EXTERNAL)
            .visibility(PortalMenuLink.PortalMenuLinkVisibility.PUBLIC)
            .order(order)
            .build();
    }
}
