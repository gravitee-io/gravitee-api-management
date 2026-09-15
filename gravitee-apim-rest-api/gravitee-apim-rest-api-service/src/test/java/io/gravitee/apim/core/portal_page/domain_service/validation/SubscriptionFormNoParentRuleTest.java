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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormNoParentRuleTest {

    private final SubscriptionFormNoParentRule rule = new SubscriptionFormNoParentRule();

    @Test
    void applies_only_to_subscription_form_area_on_create() {
        assertThat(rule.appliesTo(createItem(PortalArea.SUBSCRIPTION_FORM, null))).isTrue();
        assertThat(rule.appliesTo(createItem(PortalArea.TOP_NAVBAR, null))).isFalse();
    }

    @Test
    void accepts_subscription_form_without_parent_on_create() {
        assertThatCode(() -> rule.validate(createItem(PortalArea.SUBSCRIPTION_FORM, null), "env-1", null)).doesNotThrowAnyException();
    }

    @Test
    void rejects_subscription_form_with_parent_on_create() {
        var parentId = PortalNavigationItemId.random();

        assertThatThrownBy(() -> rule.validate(createItem(PortalArea.SUBSCRIPTION_FORM, parentId), "env-1", null))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("never have a parent");
    }

    @Test
    void applies_only_to_subscription_form_area_on_update() {
        var existingSubscriptionForm = PortalNavigationItemFixtures.aSubscriptionForm(
            "00000000-0000-0000-0000-000000000020",
            PortalPageContentId.random()
        );
        var existingPage = PortalNavigationItemFixtures.aPage("00000000-0000-0000-0000-000000000021", "Support", null);

        assertThat(rule.appliesTo(UpdatePortalNavigationItem.builder().build(), existingSubscriptionForm)).isTrue();
        assertThat(rule.appliesTo(UpdatePortalNavigationItem.builder().build(), existingPage)).isFalse();
    }

    @Test
    void rejects_subscription_form_with_parent_on_update() {
        var existingSubscriptionForm = PortalNavigationItemFixtures.aSubscriptionForm(
            "00000000-0000-0000-0000-000000000020",
            PortalPageContentId.random()
        );
        var update = UpdatePortalNavigationItem.builder().parentId(PortalNavigationItemId.random()).build();

        assertThatThrownBy(() -> rule.validate(update, existingSubscriptionForm, UpdateValidationContext.empty()))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("never have a parent");
    }

    private static CreatePortalNavigationItem createItem(PortalArea area, PortalNavigationItemId parentId) {
        return CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.SUBSCRIPTION_FORM)
            .title("Subscription Form")
            .segment("subscription-form")
            .area(area)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .parentId(parentId)
            .build();
    }
}
