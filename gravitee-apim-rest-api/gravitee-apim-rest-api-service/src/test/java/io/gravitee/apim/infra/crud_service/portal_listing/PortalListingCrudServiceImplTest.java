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
package io.gravitee.apim.infra.crud_service.portal_listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalListingFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.PortalListingAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalListingRepository;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PortalListingCrudServiceImplTest {

    PortalListingRepository repository;
    PortalListingCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PortalListingRepository.class);
        service = new PortalListingCrudServiceImpl(repository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_portal_listing() {
            var listing = PortalListingFixtures.aPortalListing();
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.create(listing);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getId()).isEqualTo(listing.getId());
                soft.assertThat(result.getEnvironmentId()).isEqualTo(listing.getEnvironmentId());
                soft.assertThat(result.getOrganizationId()).isEqualTo(listing.getOrganizationId());
                soft.assertThat(result.getPortalId()).isEqualTo(listing.getPortalId());
                soft.assertThat(result.getApis()).isEqualTo(listing.getApis());
            });
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            var listing = PortalListingFixtures.aPortalListing();
            when(repository.create(any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.create(listing));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to create a Portal Listing with id: " + listing.getId());
        }
    }

    @Nested
    class FindByIdAndEnvironmentId {

        @Test
        @SneakyThrows
        void should_return_listing_and_adapt_it() {
            var listing = PortalListingFixtures.aPortalListing();
            when(repository.findByIdAndEnvironmentId(listing.getId().toString(), listing.getEnvironmentId())).thenReturn(
                Optional.of(PortalListingAdapter.INSTANCE.toRepository(listing))
            );

            var result = service.findByIdAndEnvironmentId(listing.getId(), listing.getEnvironmentId());

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(listing);
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_not_found() {
            when(repository.findByIdAndEnvironmentId(any(), any())).thenReturn(Optional.empty());

            assertThat(service.findByIdAndEnvironmentId(PortalListingFixtures.PORTAL_LISTING_ID, "environment-id")).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            when(repository.findByIdAndEnvironmentId(any(), any())).thenThrow(TechnicalException.class);

            var listingId = PortalListingFixtures.PORTAL_LISTING_ID;
            var throwable = catchThrowable(() -> service.findByIdAndEnvironmentId(listingId, "environment-id"));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while trying to find a Portal Listing with id (" + listingId + ") in environment (environment-id)"
                );
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_existing_listing() {
            var listing = PortalListingFixtures.aPortalListing();
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.update(listing);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.PortalListing.class);
            verify(repository).update(captor.capture());
            assertThat(captor.getValue()).isEqualTo(PortalListingAdapter.INSTANCE.toRepository(listing));
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(repository.update(any())).thenThrow(TechnicalException.class);

            var listing = PortalListingFixtures.aPortalListing();
            var throwable = catchThrowable(() -> service.update(listing));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to update a Portal Listing with id: " + listing.getId());
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_listing() throws TechnicalException {
            var listingId = PortalListingFixtures.PORTAL_LISTING_ID;

            service.delete(listingId);

            verify(repository).delete(listingId.toString());
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            var listingId = PortalListingFixtures.PORTAL_LISTING_ID;
            doThrow(new TechnicalException("boom")).when(repository).delete(listingId.toString());

            assertThatThrownBy(() -> service.delete(listingId))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to delete the Portal Listing with id: " + listingId);
        }
    }
}
