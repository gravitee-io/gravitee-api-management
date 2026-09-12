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
package io.gravitee.apim.core.portal_page.query_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsQueryServiceDefaultMethodsTest {

    private static final String ENV_ID = PortalNavigationItemFixtures.ENV_ID;

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
    }

    @Test
    void returns_empty_when_no_subscription_form_is_published() {
        var form = PortalNavigationItemFixtures.aSubscriptionForm(PortalNavigationItemId.random().json(), PortalPageContentId.random())
            .toBuilder()
            .published(false)
            .build();
        navigationItemsQueryService.storage().add(form);

        assertThat(navigationItemsQueryService.findPublishedSubscriptionForm(ENV_ID)).isEmpty();
    }

    @Test
    void returns_the_item_when_exactly_one_subscription_form_is_published() {
        var published = PortalNavigationItemFixtures.aSubscriptionForm(
            PortalNavigationItemId.random().json(),
            PortalPageContentId.random()
        );
        var unpublished = PortalNavigationItemFixtures.aSubscriptionForm(
            PortalNavigationItemId.random().json(),
            PortalPageContentId.random()
        )
            .toBuilder()
            .published(false)
            .build();
        navigationItemsQueryService.storage().add(published);
        navigationItemsQueryService.storage().add(unpublished);

        assertThat(navigationItemsQueryService.findPublishedSubscriptionForm(ENV_ID)).contains(published);
    }

    @Test
    void returns_empty_when_more_than_one_subscription_form_is_published() {
        var first = PortalNavigationItemFixtures.aSubscriptionForm(PortalNavigationItemId.random().json(), PortalPageContentId.random());
        var second = PortalNavigationItemFixtures.aSubscriptionForm(PortalNavigationItemId.random().json(), PortalPageContentId.random());
        navigationItemsQueryService.storage().add(first);
        navigationItemsQueryService.storage().add(second);

        assertThat(navigationItemsQueryService.findPublishedSubscriptionForm(ENV_ID)).isEmpty();
    }
}
