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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalListingAdapterTest {

    private static final PortalListingId LISTING_ID = PortalListingId.of("00000000-0000-0000-0000-0000000000b1");
    private static final PortalId PORTAL_ID = PortalId.of("00000000-0000-0000-0000-0000000000a1");

    @Test
    void should_round_trip_through_repository_and_back() {
        var apis = List.of(
            new PortalListingApiEntry("pets-api", "/projects/alpha", 1),
            new PortalListingApiEntry("shop-api", "/projects/beta", 2)
        );
        var listing = PortalListing.of(LISTING_ID, "environment-id", "organization-id", PORTAL_ID, apis);

        var repo = PortalListingAdapter.INSTANCE.toRepository(listing);
        var back = PortalListingAdapter.INSTANCE.toEntity(repo);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(back.getId()).isEqualTo(listing.getId());
            soft.assertThat(back.getEnvironmentId()).isEqualTo(listing.getEnvironmentId());
            soft.assertThat(back.getOrganizationId()).isEqualTo(listing.getOrganizationId());
            soft.assertThat(back.getPortalId()).isEqualTo(listing.getPortalId());
            soft.assertThat(back.getApis()).isEqualTo(apis);
        });
    }

    @Test
    void should_serialize_apis_as_json_array() {
        var listing = PortalListing.of(
            LISTING_ID,
            "environment-id",
            "organization-id",
            PORTAL_ID,
            List.of(new PortalListingApiEntry("pets-api", "/projects/alpha", 1))
        );

        var repo = PortalListingAdapter.INSTANCE.toRepository(listing);

        assertThat(repo.getApis()).isEqualTo("[{\"apiHrid\":\"pets-api\",\"location\":\"/projects/alpha\",\"order\":1}]");
    }

    @Test
    void should_return_empty_list_when_apis_field_is_null() {
        var repo = io.gravitee.repository.management.model.PortalListing.builder()
            .id(LISTING_ID.toString())
            .environmentId("environment-id")
            .organizationId("organization-id")
            .portalId(PORTAL_ID.toString())
            .apis(null)
            .build();

        var entity = PortalListingAdapter.INSTANCE.toEntity(repo);

        assertThat(entity.getApis()).isEmpty();
    }

    @Test
    void should_return_empty_list_when_apis_field_is_blank() {
        var repo = io.gravitee.repository.management.model.PortalListing.builder()
            .id(LISTING_ID.toString())
            .environmentId("environment-id")
            .organizationId("organization-id")
            .portalId(PORTAL_ID.toString())
            .apis("")
            .build();

        var entity = PortalListingAdapter.INSTANCE.toEntity(repo);

        assertThat(entity.getApis()).isEmpty();
    }

    @Test
    void should_accept_null_input() {
        assertThat(PortalListingAdapter.INSTANCE.toEntity(null)).isNull();
        assertThat(PortalListingAdapter.INSTANCE.toRepository(null)).isNull();
    }
}
