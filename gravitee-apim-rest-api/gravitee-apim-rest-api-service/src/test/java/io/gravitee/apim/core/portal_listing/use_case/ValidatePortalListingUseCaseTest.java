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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.domain_service.ValidatePortalListingDomainService;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePortalListingUseCaseTest {

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

    private final ApiCrudServiceInMemory apiCrud = new ApiCrudServiceInMemory();
    private final ValidatePortalListingDomainService validator = new ValidatePortalListingDomainService(
        new PortalAutomationScopeDomainService(new PortalCrudServiceInMemory(), () -> false),
        apiCrud
    );
    private ValidatePortalListingUseCase useCase;

    @BeforeEach
    void setUp() {
        apiCrud.initWith(List.of(anApi("pets-api"), anApi("shop-api")));
        useCase = new ValidatePortalListingUseCase(validator);
    }

    @Test
    void should_return_listing_id_and_no_errors_for_well_formed_input() {
        var output = useCase.execute(
            new CreateOrUpdatePortalListingUseCase.Input(
                AUDIT_INFO,
                LISTING_ID,
                PORTAL_ID,
                List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1))
            )
        );

        assertThat(output.id()).isEqualTo(LISTING_ID);
        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_not_block_when_referenced_portal_does_not_exist() {
        // Orphan tolerance — no parent-existence check.
        var output = useCase.execute(
            new CreateOrUpdatePortalListingUseCase.Input(
                AUDIT_INFO,
                LISTING_ID,
                PORTAL_ID,
                List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1))
            )
        );

        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_surface_location_format_errors_with_indexed_field_name() {
        var output = useCase.execute(
            new CreateOrUpdatePortalListingUseCase.Input(
                AUDIT_INFO,
                LISTING_ID,
                PORTAL_ID,
                List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1), new PortalListingApiEntry("shop-api", "bad", 2))
            )
        );

        assertThat(output.errors()).anyMatch(e -> e.getMessage().contains("apis[1].location"));
    }

    @Test
    void should_surface_error_when_referenced_api_does_not_exist() {
        var output = useCase.execute(
            new CreateOrUpdatePortalListingUseCase.Input(
                AUDIT_INFO,
                LISTING_ID,
                PORTAL_ID,
                List.of(new PortalListingApiEntry("ghost-api", "/projects/alpha", 1))
            )
        );

        assertThat(output.errors()).anyMatch(e -> e.getMessage().contains("apis[0]") && e.getMessage().contains("ghost-api"));
    }

    @Test
    void should_check_api_existence_in_a_single_batched_call_for_multiple_entries() {
        ApiCrudService apiCrudSpy = spy(apiCrud);
        var spiedUseCase = new ValidatePortalListingUseCase(
            new ValidatePortalListingDomainService(
                new PortalAutomationScopeDomainService(new PortalCrudServiceInMemory(), () -> false),
                apiCrudSpy
            )
        );

        spiedUseCase.execute(
            new CreateOrUpdatePortalListingUseCase.Input(
                AUDIT_INFO,
                LISTING_ID,
                PORTAL_ID,
                List.of(
                    new PortalListingApiEntry("pets-api", "/projects/alpha", 1),
                    new PortalListingApiEntry("shop-api", "/projects/beta", 2)
                )
            )
        );

        verify(apiCrudSpy).findByIds(anyList());
        verify(apiCrudSpy, never()).existsById(any());
    }

    private static Api anApi(String hrid) {
        return Api.builder()
            .id(HRIDToUUID.api().context(AUDIT_INFO).hrid(hrid).id())
            .name(hrid)
            .environmentId(AUDIT_INFO.environmentId())
            .build();
    }
}
