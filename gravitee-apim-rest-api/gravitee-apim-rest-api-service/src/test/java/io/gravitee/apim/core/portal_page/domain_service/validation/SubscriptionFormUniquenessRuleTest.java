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

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.SubscriptionFormAlreadyPublishedException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class Create {

        @Test
        void applies_only_to_published_subscription_form_items() {
            assertThat(rule.appliesTo(subscriptionFormCreateItem(true))).isTrue();
            assertThat(rule.appliesTo(subscriptionFormCreateItem(false))).isFalse();
            assertThat(
                rule.appliesTo(
                    CreatePortalNavigationItem.builder()
                        .type(PortalNavigationItemType.FOLDER)
                        .title("Navbar")
                        .segment("navbar")
                        .area(PortalArea.TOP_NAVBAR)
                        .order(0)
                        .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                        .published(true)
                        .build()
                )
            ).isFalse();
        }

        @Test
        void rejects_creating_a_second_published_subscription_form_for_the_same_environment() {
            navigationItemsQueryService.storage().add(PortalNavigationItem.from(subscriptionFormCreateItem(true), ORG_ID, ENV_ID, null));

            assertThatThrownBy(() -> rule.validate(subscriptionFormCreateItem(true), ENV_ID, null)).isInstanceOf(
                SubscriptionFormAlreadyPublishedException.class
            );
        }

        @Test
        void allows_the_first_published_subscription_form_for_an_environment() {
            assertThatCode(() -> rule.validate(subscriptionFormCreateItem(true), ENV_ID, null)).doesNotThrowAnyException();
        }

        @Test
        void existing_published_subscription_form_in_a_different_environment_does_not_conflict() {
            navigationItemsQueryService
                .storage()
                .add(PortalNavigationItem.from(subscriptionFormCreateItem(true), ORG_ID, "env-other", null));

            assertThatCode(() -> rule.validate(subscriptionFormCreateItem(true), ENV_ID, null)).doesNotThrowAnyException();
        }
    }

    @Nested
    class Update {

        @Test
        void applies_only_to_subscription_form_items_being_published() {
            var existing = subscriptionForm(false);

            assertThat(rule.appliesTo(updatePublished(true), existing)).isTrue();
            assertThat(rule.appliesTo(updatePublished(false), existing)).isFalse();
        }

        @Test
        void rejects_publishing_a_second_form_while_another_is_already_published() {
            var alreadyPublished = subscriptionForm(true);
            var toBePublished = subscriptionForm(false);
            navigationItemsQueryService.storage().add(alreadyPublished);
            navigationItemsQueryService.storage().add(toBePublished);

            assertThatThrownBy(() -> rule.validate(updatePublished(true), toBePublished, null)).isInstanceOf(
                SubscriptionFormAlreadyPublishedException.class
            );
        }

        @Test
        void allows_republishing_the_same_form_that_is_already_the_only_published_one() {
            var alreadyPublished = subscriptionForm(true);
            navigationItemsQueryService.storage().add(alreadyPublished);

            assertThatCode(() -> rule.validate(updatePublished(true), alreadyPublished, null)).doesNotThrowAnyException();
        }

        @Test
        void allows_publishing_when_no_other_form_is_currently_published() {
            var toBePublished = subscriptionForm(false);
            navigationItemsQueryService.storage().add(toBePublished);

            assertThatCode(() -> rule.validate(updatePublished(true), toBePublished, null)).doesNotThrowAnyException();
        }

        private PortalNavigationSubscriptionForm subscriptionForm(boolean published) {
            return (PortalNavigationSubscriptionForm) PortalNavigationItem.from(
                subscriptionFormCreateItem(published),
                ORG_ID,
                ENV_ID,
                null
            );
        }

        private UpdatePortalNavigationItem updatePublished(boolean published) {
            return UpdatePortalNavigationItem.builder().title("Subscription Form").published(published).build();
        }
    }

    private static CreatePortalNavigationItem subscriptionFormCreateItem(boolean published) {
        return CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.SUBSCRIPTION_FORM)
            .title("Subscription Form")
            .segment("subscription-form")
            .area(PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .portalPageContentId(PortalPageContentId.random())
            .published(published)
            .build();
    }
}
