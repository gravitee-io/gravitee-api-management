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

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormContentTypeRuleTest {

    private PortalPageContentQueryServiceInMemory pageContentQueryService;
    private SubscriptionFormContentTypeRule rule;

    @BeforeEach
    void setUp() {
        pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        rule = new SubscriptionFormContentTypeRule(pageContentQueryService);
    }

    @Test
    void applies_only_to_subscription_form_area_with_a_content_id() {
        assertThat(rule.appliesTo(createItem(PortalArea.SUBSCRIPTION_FORM, PortalPageContentId.random()))).isTrue();
        assertThat(rule.appliesTo(createItem(PortalArea.SUBSCRIPTION_FORM, null))).isFalse();
        assertThat(rule.appliesTo(createItem(PortalArea.TOP_NAVBAR, PortalPageContentId.random()))).isFalse();
    }

    @Test
    void accepts_gravitee_markdown_content() {
        var content = PortalPageContentFixtures.aGraviteeMarkdownPageContent();
        pageContentQueryService.initWith(List.of(content));

        assertThatCode(() ->
            rule.validate(createItem(PortalArea.SUBSCRIPTION_FORM, content.getId()), "env-1", null)
        ).doesNotThrowAnyException();
    }

    @Test
    void rejects_non_gravitee_markdown_content() {
        var content = PortalPageContentFixtures.anAsyncApiPageContent();
        pageContentQueryService.initWith(List.of(content));

        assertThatThrownBy(() -> rule.validate(createItem(PortalArea.SUBSCRIPTION_FORM, content.getId()), "env-1", null))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("GRAVITEE_MARKDOWN");
    }

    @Test
    void rejects_a_missing_content_id() {
        var missingId = PortalPageContentId.random();

        assertThatThrownBy(() -> rule.validate(createItem(PortalArea.SUBSCRIPTION_FORM, missingId), "env-1", null)).isInstanceOf(
            PageContentNotFoundException.class
        );
    }

    private static CreatePortalNavigationItem createItem(PortalArea area, PortalPageContentId contentId) {
        return CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.SUBSCRIPTION_FORM)
            .title("Subscription Form")
            .segment("subscription-form")
            .area(area)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .portalPageContentId(contentId)
            .build();
    }
}
