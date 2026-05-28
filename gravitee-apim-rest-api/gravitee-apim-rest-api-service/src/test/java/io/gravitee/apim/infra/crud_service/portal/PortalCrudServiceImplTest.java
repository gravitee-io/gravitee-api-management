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
package io.gravitee.apim.infra.crud_service.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.infra.adapter.PortalAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalRepository;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PortalCrudServiceImplTest {

    PortalRepository repository;
    PortalCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PortalRepository.class);
        service = new PortalCrudServiceImpl(repository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_portal() {
            var portal = PortalFixtures.aPortal();
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.create(portal);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getId()).isEqualTo(portal.getId());
                soft.assertThat(result.getEnvironmentId()).isEqualTo(portal.getEnvironmentId());
                soft.assertThat(result.getOrganizationId()).isEqualTo(portal.getOrganizationId());
                soft.assertThat(result.getName()).isEqualTo(portal.getName());
            });
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            var portal = PortalFixtures.aPortal();
            when(repository.create(any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.create(portal));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to create a Portal with id: " + portal.getId());
        }
    }

    @Nested
    class FindByIdAndEnvironmentId {

        @Test
        @SneakyThrows
        void should_return_portal_and_adapt_it() {
            var portalId = PortalFixtures.PORTAL_ID;
            var environmentId = "environment-id";
            when(repository.findByIdAndEnvironmentId(portalId.toString(), environmentId)).thenReturn(
                Optional.of(
                    io.gravitee.repository.management.model.Portal.builder()
                        .id(portalId.toString())
                        .environmentId(environmentId)
                        .organizationId("organization-id")
                        .name("Default Portal")
                        .build()
                )
            );

            var result = service.findByIdAndEnvironmentId(portalId, environmentId);

            assertThat(result).isPresent();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.get().getId()).isEqualTo(portalId);
                soft.assertThat(result.get().getEnvironmentId()).isEqualTo(environmentId);
                soft.assertThat(result.get().getName()).isEqualTo("Default Portal");
            });
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_not_found() {
            when(repository.findByIdAndEnvironmentId(any(), any())).thenReturn(Optional.empty());

            assertThat(service.findByIdAndEnvironmentId(PortalFixtures.PORTAL_ID, "environment-id")).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            when(repository.findByIdAndEnvironmentId(any(), any())).thenThrow(TechnicalException.class);

            var portalId = PortalFixtures.PORTAL_ID;
            var throwable = catchThrowable(() -> service.findByIdAndEnvironmentId(portalId, "environment-id"));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to find a Portal with id (" + portalId + ") in environment (environment-id)");
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_existing_portal() {
            Portal portal = PortalFixtures.aPortal().toBuilder().name("Renamed").build();
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.update(portal);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Portal.class);
            verify(repository).update(captor.capture());
            assertThat(captor.getValue()).isEqualTo(PortalAdapter.INSTANCE.toRepository(portal));
            assertThat(captor.getValue().getName()).isEqualTo("Renamed");
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_portal() {
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toUpdate = PortalFixtures.aPortal();
            var result = service.update(toUpdate);

            assertThat(result).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(repository.update(any())).thenThrow(TechnicalException.class);

            var portal = PortalFixtures.aPortal();
            var throwable = catchThrowable(() -> service.update(portal));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to update a Portal with id: " + portal.getId());
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_portal() throws TechnicalException {
            var portalId = PortalFixtures.PORTAL_ID;

            service.delete(portalId);

            verify(repository).delete(portalId.toString());
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            var portalId = PortalFixtures.PORTAL_ID;
            doThrow(new TechnicalException("boom")).when(repository).delete(portalId.toString());

            assertThatThrownBy(() -> service.delete(portalId))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to delete the Portal with id: " + portalId);
            verify(repository).delete(portalId.toString());
        }
    }
}
