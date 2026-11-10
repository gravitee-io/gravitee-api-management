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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.model.PortalListing;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class PortalListingRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portallisting-tests/";
    }

    @Test
    public void should_find_by_id_and_environment() throws Exception {
        Optional<PortalListing> listing = portalListingRepository.findByIdAndEnvironmentId("portal-listing-1", "environment1");

        assertThat(listing).isPresent();
        assertThat(listing.get().getPortalId()).isEqualTo("portal1");
        assertThat(listing.get().getOrganizationId()).isEqualTo("organization1");
        assertThat(listing.get().getApis()).contains("pets-api").contains("/projects/alpha");
    }

    @Test
    public void should_not_find_by_id_when_environment_differs() throws Exception {
        Optional<PortalListing> listing = portalListingRepository.findByIdAndEnvironmentId("portal-listing-1", "environment2");

        assertThat(listing).isNotPresent();
    }

    @Test
    public void should_find_all_by_portal_and_environment() throws Exception {
        List<PortalListing> listings = portalListingRepository.findAllByPortalIdAndEnvironmentId("portal1", "environment1");

        assertThat(listings).extracting(PortalListing::getId).containsExactlyInAnyOrder("portal-listing-1", "portal-listing-2");
    }

    @Test
    public void should_find_single_portal_listing_by_portal_and_environment() throws Exception {
        List<PortalListing> listings = portalListingRepository.findAllByPortalIdAndEnvironmentId("portal2", "environment1");

        assertThat(listings).extracting(PortalListing::getId).containsExactly("portal-listing-3");
    }

    @Test
    public void should_not_find_listings_when_environment_differs() throws Exception {
        List<PortalListing> listings = portalListingRepository.findAllByPortalIdAndEnvironmentId("portal1", "environment2");

        assertThat(listings).isEmpty();
    }

    @Test
    public void should_not_find_listings_for_unknown_portal() throws Exception {
        List<PortalListing> listings = portalListingRepository.findAllByPortalIdAndEnvironmentId("portal-nope", "environment1");

        assertThat(listings).isEmpty();
    }

    @Test
    public void should_create() throws Exception {
        PortalListing toCreate = PortalListing.builder()
            .id("new-listing")
            .environmentId("environment1")
            .organizationId("organization1")
            .portalId("portal1")
            .apis("[{\"apiHrid\":\"new-api\",\"location\":\"/projects/new\",\"order\":1}]")
            .build();

        PortalListing created = portalListingRepository.create(toCreate);

        assertThat(created).isEqualTo(toCreate);
        assertThat(portalListingRepository.findById("new-listing")).hasValue(toCreate);
    }

    @Test
    public void should_update() throws Exception {
        PortalListing toUpdate = PortalListing.builder()
            .id("portal-listing-1")
            .environmentId("environment1")
            .organizationId("organization1")
            .portalId("portal1")
            .apis("[{\"apiHrid\":\"pets-api\",\"location\":\"/projects/alpha\",\"order\":2}]")
            .build();

        PortalListing updated = portalListingRepository.update(toUpdate);

        assertThat(updated.getApis()).contains("\"order\":2");
        assertThat(portalListingRepository.findById("portal-listing-1")).hasValue(toUpdate);
    }

    @Test
    public void should_delete() throws Exception {
        portalListingRepository.delete("portal-listing-1");

        assertThat(portalListingRepository.findById("portal-listing-1")).isNotPresent();
    }

    @Test
    public void should_delete_by_environment() throws Exception {
        portalListingRepository.deleteByEnvironmentId("environment1");

        Set<PortalListing> remaining = portalListingRepository.findAll();
        assertThat(remaining)
            .extracting(PortalListing::getId)
            .containsExactlyInAnyOrder(
                "portal-listing-4",
                "portal-listing-5",
                "orphan-listing-zero-uuid",
                "orphan-listing-dead-portal",
                "listing-on-existing-portal",
                "orphan-listing-other-environment"
            );
    }

    @Test
    public void should_delete_by_organization() throws Exception {
        portalListingRepository.deleteByOrganizationId("organization1");

        Set<PortalListing> remaining = portalListingRepository.findAll();
        assertThat(remaining)
            .extracting(PortalListing::getId)
            .containsExactlyInAnyOrder(
                "portal-listing-5",
                "orphan-listing-zero-uuid",
                "orphan-listing-dead-portal",
                "listing-on-existing-portal",
                "orphan-listing-other-environment"
            );
    }

    //////////////////////////////////////
    ////   ORPHAN PORTAL IDS
    //////////////////////////////////////

    private static final String ORPHANS_ENV = "environment-orphans";
    private static final List<String> EXISTING_PORTALS = List.of("existing-portal", "target-portal");

    private String portalIdOf(String listingId) throws Exception {
        return portalListingRepository.findById(listingId).orElseThrow().getPortalId();
    }

    @Test
    public void should_update_orphan_portal_ids() throws Exception {
        portalListingRepository.updateOrphanPortalIds(ORPHANS_ENV, EXISTING_PORTALS, "target-portal");

        assertThat(portalIdOf("orphan-listing-zero-uuid")).isEqualTo("target-portal");
        assertThat(portalIdOf("orphan-listing-dead-portal")).isEqualTo("target-portal");
    }

    @Test
    public void should_leave_listings_on_an_existing_portal_untouched() throws Exception {
        portalListingRepository.updateOrphanPortalIds(ORPHANS_ENV, EXISTING_PORTALS, "target-portal");

        assertThat(portalIdOf("listing-on-existing-portal")).isEqualTo("existing-portal");
    }

    @Test
    public void should_not_touch_listings_of_another_environment() throws Exception {
        portalListingRepository.updateOrphanPortalIds(ORPHANS_ENV, EXISTING_PORTALS, "target-portal");

        assertThat(portalIdOf("orphan-listing-other-environment")).isEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    public void should_only_change_the_portal_id() throws Exception {
        var before = portalListingRepository.findById("orphan-listing-zero-uuid").orElseThrow();

        portalListingRepository.updateOrphanPortalIds(ORPHANS_ENV, EXISTING_PORTALS, "target-portal");

        var after = portalListingRepository.findById("orphan-listing-zero-uuid").orElseThrow();
        assertThat(after.getPortalId()).isEqualTo("target-portal");
        assertThat(after).usingRecursiveComparison().ignoringFields("portalId").isEqualTo(before);
    }

    @Test
    public void should_return_the_number_of_listings_changed_and_zero_on_a_rerun() throws Exception {
        assertThat(portalListingRepository.updateOrphanPortalIds(ORPHANS_ENV, EXISTING_PORTALS, "target-portal")).isEqualTo(2L);
        assertThat(portalListingRepository.updateOrphanPortalIds(ORPHANS_ENV, EXISTING_PORTALS, "target-portal")).isZero();
    }
}
