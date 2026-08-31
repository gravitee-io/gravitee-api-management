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
package io.gravitee.apim.core.portal_page.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalVisibilityTest {

    @Test
    void inheritedFrom_returns_parent_visibility_when_container_present() {
        var phantom = PortalNavigationItemContainer.phantom(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001"));
        assertThat(PortalVisibility.inheritedFrom(phantom)).isEqualTo(PortalVisibility.PUBLIC);
    }

    @Test
    void inheritedFrom_defaults_to_public_when_parent_is_null() {
        assertThat(PortalVisibility.inheritedFrom(null)).isEqualTo(PortalVisibility.PUBLIC);
    }

    @Test
    void resolve_with_visibilities_returns_caller_when_present() {
        assertThat(PortalVisibility.resolve(PortalVisibility.PRIVATE, PortalVisibility.PUBLIC)).isEqualTo(PortalVisibility.PRIVATE);
        assertThat(PortalVisibility.resolve(PortalVisibility.PUBLIC, PortalVisibility.PRIVATE)).isEqualTo(PortalVisibility.PUBLIC);
    }

    @Test
    void resolve_with_visibilities_falls_back_to_parent_when_caller_is_null() {
        assertThat(PortalVisibility.resolve(null, PortalVisibility.PRIVATE)).isEqualTo(PortalVisibility.PRIVATE);
    }

    @Test
    void resolve_with_container_falls_back_to_inheritance() {
        var phantom = PortalNavigationItemContainer.phantom(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000001"));
        assertThat(PortalVisibility.resolve(null, phantom)).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(PortalVisibility.resolve(PortalVisibility.PRIVATE, phantom)).isEqualTo(PortalVisibility.PRIVATE);
    }
}
