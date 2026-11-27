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
package io.gravitee.apim.core.portal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalHostTemplateTest {

    @Test
    void should_put_the_slug_in_the_host() {
        assertThat(PortalHostTemplate.parse("{portal-slug}.localhost").hostFor("partners")).isEqualTo("partners.localhost");
    }

    @Test
    void should_accept_the_slug_inside_a_label() {
        assertThat(PortalHostTemplate.parse("dev-{portal-slug}.portals.example.com").hostFor("partners")).isEqualTo(
            "dev-partners.portals.example.com"
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "",
            "portal.example.com",
            "{portal-slug}.{portal-slug}.example.com",
            "https://{portal-slug}.example.com",
            "{portal-slug}.example.com:8443",
            "{portal-slug}.example.com/next",
            "{portal_slug}.example.com",
            "{portal-slug}..example.com",
            "-{portal-slug}.example.com",
        }
    )
    void should_refuse_an_invalid_template(String template) {
        assertThatThrownBy(() -> PortalHostTemplate.parse(template)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_refuse_a_template_leaving_no_room_for_the_longest_slug() {
        var template = "a".repeat(20) + "{portal-slug}.example.com";

        assertThatThrownBy(() -> PortalHostTemplate.parse(template)).isInstanceOf(IllegalArgumentException.class);
    }
}
