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

import io.gravitee.apim.core.portal.model.PortalId;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NavigationItemReferenceTest {

    @Test
    void default_reference_is_a_portal_reference_wrapping_portal_id_zero() {
        assertThat(NavigationItemReference.defaultReference()).isEqualTo(new NavigationItemReference.PortalReference(PortalId.ZERO));
    }

    @Test
    void default_reference_returns_the_same_instance_as_portal_reference_default() {
        assertThat(NavigationItemReference.defaultReference()).isSameAs(NavigationItemReference.PortalReference.DEFAULT);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("rootNamespacePairs")
    void shares_root_namespace_with_pins_the_root_namespace(
        NavigationItemReference one,
        NavigationItemReference other,
        String scenario,
        boolean shared
    ) {
        assertThat(one.sharesRootNamespaceWith(other)).isEqualTo(shared);
        assertThat(other.sharesRootNamespaceWith(one)).isEqualTo(shared);
    }

    private static Stream<Arguments> rootNamespacePairs() {
        var api = new NavigationItemReference.ApiReference("api-1");
        var sameApi = new NavigationItemReference.ApiReference("api-1");
        var otherApi = new NavigationItemReference.ApiReference("api-2");
        var consolePortal = NavigationItemReference.defaultReference();
        var attachedPortal = new NavigationItemReference.PortalReference(PortalId.of("44444444-4444-4444-4444-4444444444a4"));

        return Stream.of(
            Arguments.of(api, sameApi, "two references to the same API share a namespace", true),
            Arguments.of(api, otherApi, "references to different APIs do not", false),
            Arguments.of(api, consolePortal, "an API subtree never shares with the portal's own roots", false),
            Arguments.of(consolePortal, consolePortal, "the default portal reference shares with itself", true),
            Arguments.of(consolePortal, attachedPortal, "a console root and a portal-attached root share a namespace", true)
        );
    }
}
