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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_listing.use_case.CreateOrUpdatePortalListingUseCase;
import io.gravitee.apim.core.portal_listing.use_case.ValidatePortalListingUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.PortalListingState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortalListingsResourceTest extends AbstractResourceTest {

    private static final String PORTAL_HRID = "default-portal";

    @Inject
    private CreateOrUpdatePortalListingUseCase createOrUpdatePortalListingUseCase;

    @Inject
    private ValidatePortalListingUseCase validatePortalListingUseCase;

    @AfterEach
    void tearDown() {
        reset(createOrUpdatePortalListingUseCase, validatePortalListingUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portals/" + PORTAL_HRID + "/listings";
    }

    @Nested
    class DryRun {

        @Test
        void should_return_populated_state_when_validation_passes() {
            when(validatePortalListingUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalListingUseCase.Output(PortalListingId.of("00000000-0000-0000-0000-0000000000b1"), List.of())
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-listing.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(validatePortalListingUseCase).execute(any(CreateOrUpdatePortalListingUseCase.Input.class));
                verifyNoInteractions(createOrUpdatePortalListingUseCase);

                var state = response.readEntity(PortalListingState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo("default-listing");
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getId()).isEqualTo("00000000-0000-0000-0000-0000000000b1");
                    soft.assertThat(state.getApis()).hasSize(2);
                    soft.assertThat(state.getApis().get(0).getApiHrid()).isEqualTo("pets-api");
                    soft.assertThat(state.getErrors()).isNull();
                });
            }
        }

        @Test
        void should_return_state_with_errors_when_validation_fails() {
            when(validatePortalListingUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalListingUseCase.Output(
                    PortalListingId.of("00000000-0000-0000-0000-0000000000b1"),
                    List.of(Validator.Error.severe("Portal [%s] does not exist", "unknown"))
                )
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-listing.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdatePortalListingUseCase);

                var state = response.readEntity(PortalListingState.class);
                assertThat(state.getErrors()).isNotNull();
                assertThat(state.getErrors().getSevere()).hasSize(1);
                assertThat(state.getErrors().getSevere().get(0)).contains("Portal").contains("unknown");
            }
        }

        @Test
        void should_return_400_when_hrid_is_missing() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("invalid-portal-listing.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                verifyNoInteractions(validatePortalListingUseCase);
                verifyNoInteractions(createOrUpdatePortalListingUseCase);
            }
        }
    }

    @Nested
    class Run {

        @Test
        void should_create_or_update_portal_listing() {
            when(createOrUpdatePortalListingUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalListingUseCase.Output(PortalListingId.of("00000000-0000-0000-0000-0000000000b1"), List.of())
            );

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-listing.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdatePortalListingUseCase).execute(any(CreateOrUpdatePortalListingUseCase.Input.class));

                var state = response.readEntity(PortalListingState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo("00000000-0000-0000-0000-0000000000b1");
                    soft.assertThat(state.getHrid()).isEqualTo("default-listing");
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getApis()).hasSize(2);
                });
            }
        }

        @Test
        void should_return_400_when_validation_severe_error_raised() {
            when(createOrUpdatePortalListingUseCase.execute(any())).thenThrow(
                new ValidationDomainException("apis[0].location must start with '/'")
            );

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-listing.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
    }
}
