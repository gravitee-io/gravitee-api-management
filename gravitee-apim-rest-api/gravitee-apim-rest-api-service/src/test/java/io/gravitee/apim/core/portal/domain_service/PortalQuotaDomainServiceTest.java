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
package io.gravitee.apim.core.portal.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalQuota;
import io.gravitee.node.api.license.LicenseManager;
import java.net.URI;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalQuotaDomainServiceTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final LicenseManager licenseManager = mock(LicenseManager.class);
    private PortalQuotaDomainService cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        cut = new PortalQuotaDomainService(licenseManager, portalCrudService);
    }

    @Test
    void should_allow_ten_portals_with_a_universe_license() {
        givenLicenseTier("universe");

        assertThat(cut.quota(ORG_ID, ENV_ID).limit()).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = { "galaxy", "planet", "oss", "unknown" })
    void should_allow_one_portal_with_any_other_license_tier(String tier) {
        givenLicenseTier(tier);

        assertThat(cut.quota(ORG_ID, ENV_ID).limit()).isEqualTo(1);
    }

    @Test
    void should_allow_one_portal_without_a_license() {
        when(licenseManager.getOrganizationLicenseOrPlatform(ORG_ID)).thenReturn(null);

        assertThat(cut.quota(ORG_ID, ENV_ID).limit()).isEqualTo(1);
    }

    @Test
    void should_count_only_the_portals_of_the_environment() {
        givenLicenseTier("universe");
        portalCrudService.initWith(List.of(portal(ENV_ID, true), portal(ENV_ID, false), portal("env-2", true)));

        assertThat(cut.quota(ORG_ID, ENV_ID)).isEqualTo(new PortalQuota(2, 10));
    }

    private void givenLicenseTier(String tier) {
        when(licenseManager.getOrganizationLicenseOrPlatform(ORG_ID)).thenReturn(LicenseFixtures.aLicenseOfTier(tier));
    }

    @SneakyThrows
    private static Portal portal(String environmentId, boolean defaultPortal) {
        return Portal.of(
            PortalId.random(),
            environmentId,
            ORG_ID,
            "Portal",
            PortalNavigationStructure.empty(),
            null,
            defaultPortal,
            URI.create("https://portal.acme.io").toURL()
        );
    }
}
