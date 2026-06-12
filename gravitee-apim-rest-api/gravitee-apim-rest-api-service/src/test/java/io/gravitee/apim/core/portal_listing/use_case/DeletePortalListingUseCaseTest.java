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
package io.gravitee.apim.core.portal_listing.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import inmemory.PortalListingCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.exception.PortalListingNotFoundException;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeletePortalListingUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_HRID = "default-portal";
    private static final String LISTING_HRID = "default-listing";
    private static final PortalListingId LISTING_ID = PortalListingId.of(
        HRIDToUUID.portalListing().context(AUDIT_INFO).portal(PORTAL_HRID).hrid(LISTING_HRID).id()
    );

    private final PortalListingCrudServiceInMemory portalListingCrudService = new PortalListingCrudServiceInMemory();
    private DeletePortalListingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeletePortalListingUseCase(portalListingCrudService);
    }

    @AfterEach
    void tearDown() {
        portalListingCrudService.reset();
    }

    @Test
    void should_delete() {
        portalListingCrudService.initWith(List.of(aListing()));

        useCase.execute(new DeletePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID));

        assertThat(portalListingCrudService.storage()).isEmpty();
    }

    @Test
    void should_throw_when_missing() {
        var throwable = catchThrowable(() -> useCase.execute(new DeletePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID)));

        assertThat(throwable).isInstanceOf(PortalListingNotFoundException.class);
    }

    @Test
    void should_throw_when_id_exists_in_different_environment() {
        portalListingCrudService.initWith(List.of(aListing()));

        var otherEnvAudit = AuditInfo.builder()
            .organizationId("organization-id")
            .environmentId("other-env")
            .actor(AuditActor.builder().userId("user-id").build())
            .build();

        var throwable = catchThrowable(() -> useCase.execute(new DeletePortalListingUseCase.Input(otherEnvAudit, LISTING_ID)));

        assertThat(throwable).isInstanceOf(PortalListingNotFoundException.class);
        assertThat(portalListingCrudService.storage()).hasSize(1);
    }

    private static PortalListing aListing() {
        var portalId = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
        return PortalListing.of(
            LISTING_ID,
            AUDIT_INFO.environmentId(),
            AUDIT_INFO.organizationId(),
            portalId,
            List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1))
        );
    }
}
