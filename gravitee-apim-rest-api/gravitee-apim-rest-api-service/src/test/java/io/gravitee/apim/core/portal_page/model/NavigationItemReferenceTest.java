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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NavigationItemReferenceTest {

    @Test
    void defaultReference_is_a_PortalReference_wrapping_PortalId_ZERO() {
        assertThat(NavigationItemReference.defaultReference()).isEqualTo(new NavigationItemReference.PortalReference(PortalId.ZERO));
    }

    @Test
    void defaultReference_returns_the_same_instance_as_PortalReference_DEFAULT() {
        assertThat(NavigationItemReference.defaultReference()).isSameAs(NavigationItemReference.PortalReference.DEFAULT);
    }
}
