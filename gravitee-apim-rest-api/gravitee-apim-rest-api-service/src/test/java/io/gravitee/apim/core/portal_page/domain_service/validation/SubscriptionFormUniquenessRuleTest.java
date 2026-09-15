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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.SubscriptionFormAlreadyExistsException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormUniquenessRuleTest {

    private static final String ENV_ID = "env-1";
    private static final String ORG_ID = "org-1";

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private SubscriptionFormUniquenessRule rule;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        rule = new SubscriptionFormUniquenessRule(navigationItemsQueryService);
    }

    @Test
    void applies_only_to_subscription_form_area() {
        assertThat(rule.appliesTo(subscriptionFormCreateItem())).isTrue();
        assertThat(
            rule.appliesTo(
                CreatePortalNavigationItem.builder()
                    .type(PortalNavigationItemType.FOLDER)
                    .title("Navbar")
                    .segment("navbar")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                    .build()
            )
        ).isFalse();
    }

    @Test
    void rejects_a_second_subscription_form_for_the_same_environment() {
        navigationItemsQueryService.storage().add(PortalNavigationItem.from(subscriptionFormCreateItem(), ORG_ID, ENV_ID, null));

        assertThatThrownBy(() -> rule.validate(subscriptionFormCreateItem(), ENV_ID, null)).isInstanceOf(
            SubscriptionFormAlreadyExistsException.class
        );
    }

    @Test
    void allows_the_first_subscription_form_for_an_environment() {
        assertThatCode(() -> rule.validate(subscriptionFormCreateItem(), ENV_ID, null)).doesNotThrowAnyException();
    }

    @Test
    void existing_subscription_form_in_a_different_environment_does_not_conflict() {
        navigationItemsQueryService.storage().add(PortalNavigationItem.from(subscriptionFormCreateItem(), ORG_ID, "env-other", null));

        assertThatCode(() -> rule.validate(subscriptionFormCreateItem(), ENV_ID, null)).doesNotThrowAnyException();
    }

    private static CreatePortalNavigationItem subscriptionFormCreateItem() {
        return CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.SUBSCRIPTION_FORM)
            .title("Subscription Form")
            .segment("subscription-form")
            .area(PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .portalPageContentId(PortalPageContentId.random())
            .build();
    }
}
