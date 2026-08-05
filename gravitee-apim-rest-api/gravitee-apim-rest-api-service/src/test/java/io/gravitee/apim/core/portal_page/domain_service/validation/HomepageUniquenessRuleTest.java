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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HomepageUniquenessRuleTest {

    private static final String ENV_ID = "env-1";
    private static final String ORG_ID = "org-1";

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private HomepageUniquenessRule rule;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        rule = new HomepageUniquenessRule(navigationItemsQueryService);
    }

    @Test
    void applies_only_to_homepage_area() {
        var homepageItem = homepageCreateItem(null);
        var navbarItem = CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.FOLDER)
            .title("Navbar")
            .segment("navbar")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();

        assertThatCode(() -> rule.appliesTo(homepageItem)).doesNotThrowAnyException();
        assertThatCode(() -> rule.appliesTo(navbarItem)).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThat(rule.appliesTo(homepageItem)).isTrue();
        org.assertj.core.api.Assertions.assertThat(rule.appliesTo(navbarItem)).isFalse();
    }

    @Test
    void two_console_owned_homepages_conflict() {
        navigationItemsQueryService.storage().add(PortalNavigationItem.from(homepageCreateItem(null), ORG_ID, ENV_ID, null));

        assertThatThrownBy(() -> rule.validate(homepageCreateItem(null), ENV_ID, null)).isInstanceOf(HomepageAlreadyExistsException.class);
    }

    @Test
    void two_portal_attached_homepages_for_same_portal_conflict() {
        navigationItemsQueryService
            .storage()
            .add(PortalNavigationItem.from(homepageCreateItem("11111111-1111-1111-1111-1111111111a1"), ORG_ID, ENV_ID, null));

        assertThatThrownBy(() -> rule.validate(homepageCreateItem("11111111-1111-1111-1111-1111111111a1"), ENV_ID, null)).isInstanceOf(
            HomepageAlreadyExistsException.class
        );
    }

    @Test
    void portal_attached_homepages_for_different_portals_do_not_conflict() {
        navigationItemsQueryService
            .storage()
            .add(PortalNavigationItem.from(homepageCreateItem("11111111-1111-1111-1111-1111111111a1"), ORG_ID, ENV_ID, null));

        assertThatCode(() ->
            rule.validate(homepageCreateItem("11111111-1111-1111-1111-1111111111b1"), ENV_ID, null)
        ).doesNotThrowAnyException();
    }

    @Test
    void console_owned_and_portal_attached_homepages_do_not_conflict() {
        navigationItemsQueryService.storage().add(PortalNavigationItem.from(homepageCreateItem(null), ORG_ID, ENV_ID, null));

        assertThatCode(() ->
            rule.validate(homepageCreateItem("11111111-1111-1111-1111-1111111111a1"), ENV_ID, null)
        ).doesNotThrowAnyException();
    }

    @Test
    void existing_homepage_in_different_env_does_not_conflict() {
        navigationItemsQueryService.storage().add(PortalNavigationItem.from(homepageCreateItem(null), ORG_ID, "env-other", null));

        assertThatCode(() -> rule.validate(homepageCreateItem(null), ENV_ID, null)).doesNotThrowAnyException();
    }

    private static CreatePortalNavigationItem homepageCreateItem(String referenceId) {
        var builder = CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.FOLDER)
            .title("Home")
            .segment("home")
            .area(PortalArea.HOMEPAGE)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN);
        if (referenceId != null) {
            builder.reference(new NavigationItemReference.PortalReference(PortalId.of(referenceId)));
        }
        return builder.build();
    }
}
