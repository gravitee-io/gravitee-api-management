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

import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalListingCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.domain_service.ValidatePortalListingDomainService;
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
class CreateOrUpdatePortalListingUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_HRID = "default-portal";
    private static final String LISTING_HRID = "default-listing";
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final PortalListingId LISTING_ID = PortalListingId.of(
        HRIDToUUID.portalListing().context(AUDIT_INFO).portal(PORTAL_HRID).hrid(LISTING_HRID).id()
    );

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final PortalListingCrudServiceInMemory portalListingCrudService = new PortalListingCrudServiceInMemory();
    private final ValidatePortalListingDomainService validator = new ValidatePortalListingDomainService(
        new PortalAutomationScopeDomainService(portalCrudService)
    );
    private CreateOrUpdatePortalListingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateOrUpdatePortalListingUseCase(
            validator,
            portalListingCrudService,
            org.mockito.Mockito.mock(io.gravitee.apim.core.portal_listing.domain_service.PortalListingSyncDomainService.class)
        );
    }

    @AfterEach
    void tearDown() {
        portalListingCrudService.reset();
    }

    @Test
    void should_create_when_not_existing() {
        var apis = List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1));

        var output = useCase.execute(new CreateOrUpdatePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID, PORTAL_ID, apis));

        assertThat(output.id()).isEqualTo(LISTING_ID);
        assertThat(output.errors()).isEmpty();
        assertThat(portalListingCrudService.storage()).hasSize(1);
        assertThat(portalListingCrudService.storage().get(0).getApis()).isEqualTo(apis);
    }

    @Test
    void should_update_when_existing() {
        useCase.execute(
            new CreateOrUpdatePortalListingUseCase.Input(
                AUDIT_INFO,
                LISTING_ID,
                PORTAL_ID,
                List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1))
            )
        );

        var updatedApis = List.of(
            new PortalListingApiEntry("pets-api", "/projects/alpha", 1),
            new PortalListingApiEntry("shop-api", "/projects/beta", 2)
        );
        var output = useCase.execute(new CreateOrUpdatePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID, PORTAL_ID, updatedApis));

        assertThat(output.id()).isEqualTo(LISTING_ID);
        assertThat(portalListingCrudService.storage()).hasSize(1);
        assertThat(portalListingCrudService.storage().get(0).getApis()).isEqualTo(updatedApis);
    }

    @Test
    void should_be_idempotent_when_put_twice() {
        var input = new CreateOrUpdatePortalListingUseCase.Input(
            AUDIT_INFO,
            LISTING_ID,
            PORTAL_ID,
            List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1))
        );

        var first = useCase.execute(input);
        var second = useCase.execute(input);

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(portalListingCrudService.storage()).hasSize(1);
    }

    @Test
    void should_persist_listing_under_the_provided_id() {
        useCase.execute(
            new CreateOrUpdatePortalListingUseCase.Input(
                AUDIT_INFO,
                LISTING_ID,
                PORTAL_ID,
                List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1))
            )
        );

        var persisted = portalListingCrudService.storage().get(0);
        assertThat(persisted.getId()).isEqualTo(LISTING_ID);
        assertThat(persisted.getPortalId()).isEqualTo(PORTAL_ID);
    }

    @Test
    void should_persist_even_when_parent_portal_does_not_exist() {
        // Orphan tolerance: no portal-existence check; the listing is written and waits for its parent.
        var apis = List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1));

        var output = useCase.execute(new CreateOrUpdatePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID, PORTAL_ID, apis));

        assertThat(output.errors()).isEmpty();
        assertThat(portalListingCrudService.storage()).hasSize(1);
    }

    @Test
    void should_throw_validation_error_when_location_is_malformed() {
        var apis = List.of(new PortalListingApiEntry("pets-api", "projects/alpha", 1));

        var throwable = catchThrowable(() ->
            useCase.execute(new CreateOrUpdatePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID, PORTAL_ID, apis))
        );

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        assertThat(throwable.getMessage()).contains("apis[0].location");
        assertThat(portalListingCrudService.storage()).isEmpty();
    }

    @Test
    void should_accept_empty_apis_list() {
        var output = useCase.execute(new CreateOrUpdatePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID, PORTAL_ID, List.of()));

        assertThat(output.errors()).isEmpty();
        assertThat(portalListingCrudService.storage()).hasSize(1);
        assertThat(portalListingCrudService.storage().get(0).getApis()).isEmpty();
    }

    @Test
    void should_treat_null_apis_as_empty() {
        var output = useCase.execute(new CreateOrUpdatePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID, PORTAL_ID, null));

        assertThat(output.errors()).isEmpty();
        assertThat(portalListingCrudService.storage().get(0).getApis()).isEmpty();
    }

    @Test
    void should_reject_portal_when_environment_already_has_a_different_portal() {
        var establishedCrud = new PortalCrudServiceInMemory();
        establishedCrud.initWith(List.of(Portal.of(PORTAL_ID, AUDIT_INFO.environmentId(), AUDIT_INFO.organizationId(), "Established")));
        var restrictedUseCase = new CreateOrUpdatePortalListingUseCase(
            new ValidatePortalListingDomainService(new PortalAutomationScopeDomainService(establishedCrud)),
            portalListingCrudService
        );
        var nonDefaultPortalId = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid("foo-portal").id());
        var apis = List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1));

        var throwable = catchThrowable(() ->
            restrictedUseCase.execute(new CreateOrUpdatePortalListingUseCase.Input(AUDIT_INFO, LISTING_ID, nonDefaultPortalId, apis))
        );

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        assertThat(throwable.getMessage()).contains("portalHrid").contains("established portal");
        assertThat(portalListingCrudService.storage()).isEmpty();
    }
}
