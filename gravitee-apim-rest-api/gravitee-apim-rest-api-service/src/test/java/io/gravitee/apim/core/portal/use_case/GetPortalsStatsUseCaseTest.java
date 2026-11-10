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
package io.gravitee.apim.core.portal.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.portal.domain_service.PortalLogoDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalQuotaDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalQuota;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.node.api.license.LicenseManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalsStatsUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final List<PortalNavigationItem> navigationItems = new ArrayList<>();
    private final LicenseManager licenseManager = mock(LicenseManager.class);
    private GetPortalsStatsUseCase cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        navigationItems.clear();
        when(licenseManager.getOrganizationLicenseOrPlatform(ORG_ID)).thenReturn(LicenseFixtures.aLicenseOfTier("universe"));
        cut = new GetPortalsStatsUseCase(
            portalCrudService,
            new PortalQuotaDomainService(licenseManager, portalCrudService),
            new PortalSummaryDomainService(
                new PortalLogoDomainService(new ThemeQueryServiceInMemory()),
                new PortalNavigationItemsQueryServiceInMemory(navigationItems)
            )
        );
    }

    @Test
    void should_return_the_default_portal_quota_and_published_apis() {
        var defaultPortal = portal(ENV_ID, true);
        portalCrudService.initWith(List.of(defaultPortal));
        navigationItems.addAll(List.of(publishedApi(defaultPortal.getId(), "api-1"), publishedApi(defaultPortal.getId(), "api-2")));

        var result = cut.execute(new GetPortalsStatsUseCase.Input(ORG_ID, ENV_ID));

        assertThat(result.quota()).isEqualTo(new PortalQuota(1, 10));
        assertThat(result.publishedApisCount()).isEqualTo(2);
    }

    @Test
    void should_count_an_api_published_on_several_portals_once() {
        var defaultPortal = portal(ENV_ID, true);
        var partners = portal(ENV_ID, false);
        portalCrudService.initWith(List.of(defaultPortal, partners, portal("env-2", true)));
        navigationItems.addAll(
            List.of(
                publishedApi(defaultPortal.getId(), "api-1"),
                publishedApi(partners.getId(), "api-1"),
                publishedApi(partners.getId(), "api-2")
            )
        );

        var result = cut.execute(new GetPortalsStatsUseCase.Input(ORG_ID, ENV_ID));

        assertThat(result.quota()).isEqualTo(new PortalQuota(2, 10));
        assertThat(result.publishedApisCount()).isEqualTo(2);
    }

    private static PortalNavigationApi publishedApi(PortalId portalId, String apiId) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .title(apiId)
            .segment(apiId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
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
