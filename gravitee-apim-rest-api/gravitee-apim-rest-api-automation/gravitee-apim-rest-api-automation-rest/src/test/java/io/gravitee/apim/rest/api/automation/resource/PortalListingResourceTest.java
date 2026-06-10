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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.exception.PortalListingNotFoundException;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_listing.use_case.DeletePortalListingUseCase;
import io.gravitee.apim.core.portal_listing.use_case.GetPortalListingUseCase;
import io.gravitee.apim.rest.api.automation.model.PortalListingState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortalListingResourceTest extends AbstractResourceTest {

    private static final String PORTAL_HRID = "default-portal";
    private static final String LISTING_HRID = "default-listing";

    @Inject
    private GetPortalListingUseCase getPortalListingUseCase;

    @Inject
    private DeletePortalListingUseCase deletePortalListingUseCase;

    @AfterEach
    void tearDown() {
        reset(getPortalListingUseCase, deletePortalListingUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portals/" + PORTAL_HRID + "/listings";
    }

    @Nested
    class Get {

        @Test
        void should_return_portal_listing() {
            var stored = aListing();
            when(getPortalListingUseCase.execute(any())).thenReturn(new GetPortalListingUseCase.Output(stored));

            try (var response = rootTarget(LISTING_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(getPortalListingUseCase).execute(any(GetPortalListingUseCase.Input.class));

                var state = response.readEntity(PortalListingState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(LISTING_HRID);
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                    soft.assertThat(state.getApis()).hasSize(1);
                });
            }
        }

        @Test
        void should_return_404_when_listing_is_missing() {
            when(getPortalListingUseCase.execute(any())).thenThrow(new PortalListingNotFoundException(LISTING_HRID));

            try (var response = rootTarget(LISTING_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_portal_listing() {
            try (var response = rootTarget(LISTING_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deletePortalListingUseCase).execute(any(DeletePortalListingUseCase.Input.class));
            }
        }

        @Test
        void should_return_404_when_listing_is_missing() {
            org.mockito.Mockito.doThrow(new PortalListingNotFoundException(LISTING_HRID)).when(deletePortalListingUseCase).execute(any());

            try (var response = rootTarget(LISTING_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    private static PortalListing aListing() {
        return PortalListing.of(
            PortalListingId.of("00000000-0000-0000-0000-0000000000b1"),
            ENVIRONMENT,
            ORGANIZATION,
            PortalId.of("00000000-0000-0000-0000-0000000000a1"),
            List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1))
        );
    }
}
