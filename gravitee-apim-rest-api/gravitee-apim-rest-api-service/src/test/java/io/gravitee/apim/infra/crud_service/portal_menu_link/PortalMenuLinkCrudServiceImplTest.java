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
package io.gravitee.apim.infra.crud_service.portal_menu_link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalMenuLinkFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_menu_link.exception.PortalMenuLinkNotFoundException;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkType;
import io.gravitee.apim.infra.adapter.PortalMenuLinkAdapter;
import io.gravitee.apim.infra.adapter.PortalMenuLinkAdapterImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalMenuLinkRepository;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class PortalMenuLinkCrudServiceImplTest {

    PortalMenuLinkRepository repository;
    PortalMenuLinkAdapter mapper;
    PortalMenuLinkCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PortalMenuLinkRepository.class);

        mapper = new PortalMenuLinkAdapterImpl();

        service = new PortalMenuLinkCrudServiceImpl(repository, mapper);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_PortalMenuLink() {
            // Given
            var portalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink();
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            var result = service.create(portalMenuLink);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getId()).isNotNull();
                soft.assertThat(result.getName()).isEqualTo(portalMenuLink.getName());
                soft.assertThat(result.getEnvironmentId()).isEqualTo(portalMenuLink.getEnvironmentId());
                soft.assertThat(result.getTarget()).isEqualTo(portalMenuLink.getTarget());
                soft.assertThat(result.getType()).isEqualTo(portalMenuLink.getType());
                soft.assertThat(result.getOrder()).isEqualTo(portalMenuLink.getOrder());
            });
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            var portalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink();
            when(repository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(portalMenuLink));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to create a PortalMenuLink with id: " + portalMenuLink.getId());
        }
    }

    @Nested
    class Get {

        @Test
        @SneakyThrows
        void should_return_PortalMenuLink_and_adapt_it() {
            // Given
            var portalMenuLinkId = "portalMenuLink-id";
            var environmentId = "portalMenuLink-environmentId";
            var portalMenuLinkInRepositoryBuilder = io.gravitee.repository.management.model.PortalMenuLink
                .builder()
                .id(portalMenuLinkId)
                .environmentId(environmentId)
                .name("portalMenuLink-name")
                .target("portalMenuLink-target")
                .type(io.gravitee.repository.management.model.PortalMenuLink.PortalMenuLinkType.EXTERNAL)
                .order(1);

            when(repository.findByIdAndEnvironmentId(portalMenuLinkId, environmentId))
                .thenAnswer(invocation ->
                    Optional.of(
                        portalMenuLinkInRepositoryBuilder.id(invocation.getArgument(0)).environmentId(invocation.getArgument(1)).build()
                    )
                );

            // When
            var result = service.getByIdAndEnvironmentId(portalMenuLinkId, environmentId);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getId()).isEqualTo(portalMenuLinkId);
                soft.assertThat(result.getEnvironmentId()).isEqualTo(environmentId);
                soft.assertThat(result.getName()).isEqualTo("portalMenuLink-name");
                soft.assertThat(result.getTarget()).isEqualTo("portalMenuLink-target");
                soft.assertThat(result.getType()).isEqualTo(PortalMenuLinkType.EXTERNAL);
            });
        }

        @Test
        @SneakyThrows
        void should_throw_when_no_PortalMenuLink_found() {
            // Given
            var portalMenuLinkId = "portalMenuLink-id";
            var environmentId = "portalMenuLink-environmentId";
            when(repository.findById(portalMenuLinkId)).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> service.getByIdAndEnvironmentId(portalMenuLinkId, environmentId));

            // Then
            assertThat(throwable)
                .isInstanceOf(PortalMenuLinkNotFoundException.class)
                .hasMessage("PortalMenuLink [ " + portalMenuLinkId + " ] not found");
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            var portalMenuLinkId = "portalMenuLink-id";
            var environmentId = "portalMenuLink-environmentId";
            when(repository.findByIdAndEnvironmentId(portalMenuLinkId, environmentId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.getByIdAndEnvironmentId(portalMenuLinkId, environmentId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while trying to find a PortalMenuLink with id (portalMenuLink-id) in environment (portalMenuLink-environmentId)"
                );
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_an_existing_PortalMenuLink() {
            PortalMenuLink portalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink();
            portalMenuLink.setName("newName");
            service.update(portalMenuLink);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.PortalMenuLink.class);
            verify(repository).update(captor.capture());

            assertThat(captor.getValue()).isEqualTo(mapper.toRepository(portalMenuLink));
            assertThat(captor.getValue().getName()).isEqualTo("newName");
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_PortalMenuLink() {
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toUpdate = PortalMenuLinkFixtures.aPortalMenuLink();
            var result = service.update(toUpdate);

            assertThat(result).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(repository.update(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.update(PortalMenuLinkFixtures.aPortalMenuLink()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to update a PortalMenuLink with id: portalMenuLinkId");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_PortalMenuLink() throws TechnicalException {
            var portalMenuLinkId = "portalMenuLink-id";
            service.delete(portalMenuLinkId);
            verify(repository).delete(portalMenuLinkId);
        }

        @Test
        void should_throw_if_deletion_problem_occurs() throws TechnicalException {
            var portalMenuLinkId = "portalMenuLink-id";
            doThrow(new TechnicalException("exception")).when(repository).delete(portalMenuLinkId);
            assertThatThrownBy(() -> service.delete(portalMenuLinkId))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to delete the PortalMenuLink with id: " + portalMenuLinkId);
            verify(repository).delete(portalMenuLinkId);
        }
    }
}
