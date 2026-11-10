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

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalSummary;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalSummaryDomainServiceTest {

    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final String ORG_ID = "org-1";
    private static final PortalId PORTAL_ID = PortalId.of("00000000-0000-0000-0000-0000000000a1");
    private static final PortalId OTHER_PORTAL_ID = PortalId.of("00000000-0000-0000-0000-0000000000a2");

    private final ThemeQueryServiceInMemory themeQueryService = new ThemeQueryServiceInMemory();
    private final List<PortalNavigationItem> navigationItems = new ArrayList<>();
    private PortalSummaryDomainService cut;

    @BeforeEach
    void setUp() {
        themeQueryService.reset();
        navigationItems.clear();
        cut = new PortalSummaryDomainService(
            new PortalLogoDomainService(themeQueryService),
            new PortalNavigationItemsQueryServiceInMemory(navigationItems)
        );
    }

    @Nested
    class PublishedApisCount {

        @Test
        void should_count_the_distinct_apis_of_the_portal_published_items() {
            navigationItems.addAll(
                List.of(
                    api(PORTAL_ID, ENV_ID, "api-1", null, true),
                    api(PORTAL_ID, ENV_ID, "api-1", null, true),
                    api(PORTAL_ID, ENV_ID, "api-2", null, true)
                )
            );

            assertThat(summaryOf(portal(PORTAL_ID, false)).publishedApisCount()).isEqualTo(2);
        }

        @Test
        void should_ignore_items_of_another_portal_or_of_the_placeholder_portal() {
            navigationItems.addAll(
                List.of(api(OTHER_PORTAL_ID, ENV_ID, "api-1", null, true), api(PortalId.ZERO, ENV_ID, "api-2", null, true))
            );

            assertThat(summaryOf(portal(PORTAL_ID, true)).publishedApisCount()).isZero();
        }

        @Test
        void should_ignore_items_of_another_environment() {
            navigationItems.add(api(PORTAL_ID, OTHER_ENV_ID, "api-1", null, true));

            assertThat(summaryOf(portal(PORTAL_ID, false)).publishedApisCount()).isZero();
        }

        @Test
        void should_ignore_an_unpublished_api_item() {
            navigationItems.add(api(PORTAL_ID, ENV_ID, "api-1", null, false));

            assertThat(summaryOf(portal(PORTAL_ID, false)).publishedApisCount()).isZero();
        }

        @Test
        void should_ignore_an_api_under_an_unpublished_ancestor() {
            var unpublishedFolder = folder(PORTAL_ID, null, false);
            var publishedSubFolder = folder(PORTAL_ID, unpublishedFolder.getId(), true);
            var publishedFolder = folder(PORTAL_ID, null, true);
            navigationItems.addAll(
                List.of(
                    unpublishedFolder,
                    publishedSubFolder,
                    publishedFolder,
                    api(PORTAL_ID, ENV_ID, "hidden-api", publishedSubFolder.getId(), true),
                    api(PORTAL_ID, ENV_ID, "visible-api", publishedFolder.getId(), true)
                )
            );

            assertThat(summaryOf(portal(PORTAL_ID, false)).publishedApisCount()).isOne();
        }

        @Test
        void should_ignore_an_api_under_an_unpublished_api_product() {
            var unpublishedProduct = apiProduct(PORTAL_ID, false);
            navigationItems.addAll(List.of(unpublishedProduct, api(PORTAL_ID, ENV_ID, "api-1", unpublishedProduct.getId(), true)));

            assertThat(summaryOf(portal(PORTAL_ID, false)).publishedApisCount()).isZero();
        }

        @Test
        void should_not_count_api_products_themselves() {
            navigationItems.add(apiProduct(PORTAL_ID, true));

            assertThat(summaryOf(portal(PORTAL_ID, false)).publishedApisCount()).isZero();
        }

        @Test
        void should_count_each_portal_separately() {
            navigationItems.addAll(
                List.of(
                    api(PORTAL_ID, ENV_ID, "api-1", null, true),
                    api(OTHER_PORTAL_ID, ENV_ID, "api-1", null, true),
                    api(OTHER_PORTAL_ID, ENV_ID, "api-2", null, true)
                )
            );

            var summaries = cut.summarize(ENV_ID, List.of(portal(PORTAL_ID, true), portal(OTHER_PORTAL_ID, false)));

            assertThat(summaries).extracting(PortalSummary::publishedApisCount).containsExactly(1, 2);
        }
    }

    @Nested
    class Logo {

        @Test
        void should_have_a_logo_when_the_enabled_next_gen_theme_has_one() {
            themeQueryService.initWith(List.of(theme(ThemeType.PORTAL_NEXT, ENV_ID, true, "data:image/png;base64,AAA")));

            assertThat(summaryOf(portal(PORTAL_ID, false)).hasLogo()).isTrue();
        }

        @Test
        void should_have_no_logo_when_the_environment_has_no_portal_logo() {
            themeQueryService.initWith(List.of(theme(ThemeType.PORTAL_NEXT, ENV_ID, false, "data:image/png;base64,AAA")));

            assertThat(summaryOf(portal(PORTAL_ID, false)).hasLogo()).isFalse();
        }
    }

    private PortalSummary summaryOf(Portal portal) {
        var summaries = cut.summarize(ENV_ID, List.of(portal));
        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().portal()).isEqualTo(portal);
        return summaries.getFirst();
    }

    private static Portal portal(PortalId id, boolean defaultPortal) {
        return Portal.of(
            id,
            ENV_ID,
            ORG_ID,
            "Portal " + id,
            PortalNavigationStructure.empty(),
            null,
            defaultPortal,
            url("https://partners.acme.io")
        );
    }

    private static PortalNavigationApi api(
        PortalId portalId,
        String environmentId,
        String apiId,
        PortalNavigationItemId parentId,
        boolean published
    ) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(environmentId)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .title(apiId)
            .segment(apiId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .parentId(parentId)
            .published(published)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static PortalNavigationFolder folder(PortalId portalId, PortalNavigationItemId parentId, boolean published) {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .title("folder")
            .segment("folder")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .published(published)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static PortalNavigationApiProduct apiProduct(PortalId portalId, boolean published) {
        return PortalNavigationApiProduct.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .title("product")
            .segment("product")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiProductId("product-1")
            .published(published)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static Theme theme(ThemeType type, String environmentId, boolean enabled, String logo) {
        return Theme.builder()
            .id(type + "-" + environmentId + "-" + enabled)
            .type(type)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(environmentId)
            .enabled(enabled)
            .logo(logo)
            .build();
    }

    @SneakyThrows
    private static URL url(String value) {
        return URI.create(value).toURL();
    }
}
