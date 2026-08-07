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

import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.portal_page.exception.InvalidUrlFormatException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LinkUrlRuleTest {

    private final LinkUrlRule rule = new LinkUrlRule();
    private final PortalNavigationLink existingLink = PortalNavigationItemFixtures.aLink();

    @Test
    void should_accept_an_https_url_on_create() {
        var item = CreatePortalNavigationItem.builder().url("https://docs.example.com").build();

        assertThatCode(() -> rule.validate(item, "environment-id", CreateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void should_accept_an_http_url_with_a_path_on_create() {
        var item = CreatePortalNavigationItem.builder().url("http://example.com/path").build();

        assertThatCode(() -> rule.validate(item, "environment-id", CreateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_relative_path_on_create() {
        var item = CreatePortalNavigationItem.builder().url("/relative/path").build();

        assertThatThrownBy(() -> rule.validate(item, "environment-id", CreateValidationContext.empty())).isInstanceOf(
            InvalidUrlFormatException.class
        );
    }

    @Test
    void should_reject_a_string_with_no_scheme_on_create() {
        var item = CreatePortalNavigationItem.builder().url("invalid-url").build();

        assertThatThrownBy(() -> rule.validate(item, "environment-id", CreateValidationContext.empty())).isInstanceOf(
            InvalidUrlFormatException.class
        );
    }

    @Test
    void should_reject_blank_on_create() {
        var item = CreatePortalNavigationItem.builder().url("  ").build();

        assertThatThrownBy(() -> rule.validate(item, "environment-id", CreateValidationContext.empty())).isInstanceOf(
            InvalidUrlFormatException.class
        );
    }

    @Test
    void should_reject_null_on_create() {
        var item = CreatePortalNavigationItem.builder().url(null).build();

        assertThatThrownBy(() -> rule.validate(item, "environment-id", CreateValidationContext.empty())).isInstanceOf(
            InvalidUrlFormatException.class
        );
    }

    @Test
    void should_accept_a_well_formed_url_on_update() {
        var toUpdate = UpdatePortalNavigationItem.builder().url("https://renamed.example.com").build();

        assertThatCode(() -> rule.validate(toUpdate, existingLink, UpdateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_malformed_url_on_update() {
        var toUpdate = UpdatePortalNavigationItem.builder().url("not-a-url").build();

        assertThatThrownBy(() -> rule.validate(toUpdate, existingLink, UpdateValidationContext.empty())).isInstanceOf(
            InvalidUrlFormatException.class
        );
    }
}
