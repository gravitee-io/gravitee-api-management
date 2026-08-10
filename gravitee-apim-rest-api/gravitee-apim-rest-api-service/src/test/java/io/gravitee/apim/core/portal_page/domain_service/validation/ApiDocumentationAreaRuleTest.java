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

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationAreaRuleTest {

    private final ApiDocumentationAreaRule rule = new ApiDocumentationAreaRule();

    @Test
    void applies_to_page_with_api_reference() {
        assertThat(rule.appliesTo(item(PortalNavigationItemType.PAGE, PortalArea.TOP_NAVBAR, apiRef()))).isTrue();
    }

    @Test
    void does_not_apply_to_page_with_portal_reference() {
        assertThat(rule.appliesTo(item(PortalNavigationItemType.PAGE, PortalArea.TOP_NAVBAR, portalRef()))).isFalse();
    }

    @Test
    void does_not_apply_to_non_page_types() {
        assertThat(rule.appliesTo(item(PortalNavigationItemType.FOLDER, PortalArea.TOP_NAVBAR, apiRef()))).isFalse();
        assertThat(rule.appliesTo(item(PortalNavigationItemType.API, PortalArea.TOP_NAVBAR, apiRef()))).isFalse();
    }

    @Test
    void accepts_top_navbar() {
        assertThatCode(() ->
            rule.validate(item(PortalNavigationItemType.PAGE, PortalArea.TOP_NAVBAR, apiRef()), "env-1", null)
        ).doesNotThrowAnyException();
    }

    @Test
    void rejects_homepage() {
        assertThatThrownBy(() -> rule.validate(item(PortalNavigationItemType.PAGE, PortalArea.HOMEPAGE, apiRef()), "env-1", null))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("TOP_NAVBAR");
    }

    private static CreatePortalNavigationItem item(PortalNavigationItemType type, PortalArea area, NavigationItemReference reference) {
        return CreatePortalNavigationItem.builder()
            .type(type)
            .title("Doc")
            .segment("doc")
            .area(area)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .reference(reference)
            .build();
    }

    private static NavigationItemReference apiRef() {
        return new NavigationItemReference.ApiReference("api-1");
    }

    private static NavigationItemReference portalRef() {
        return new NavigationItemReference.PortalReference(PortalId.of("11111111-1111-1111-1111-1111111111a1"));
    }
}
