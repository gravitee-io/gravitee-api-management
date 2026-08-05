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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalIdTest {

    private static final String ZERO_UUID = "00000000-0000-0000-0000-000000000000";

    @Test
    void of_returns_ZERO_constant_for_zero_uuid() {
        assertThat(PortalId.of(ZERO_UUID)).isSameAs(PortalId.ZERO);
    }

    @Test
    void of_returns_non_zero_instance_for_other_uuid() {
        var other = PortalId.of("11111111-1111-1111-1111-111111111111");
        assertThat(other).isNotSameAs(PortalId.ZERO);
        assertThat(other).isNotEqualTo(PortalId.ZERO);
    }

    @Test
    void ZERO_toString_is_the_zero_uuid() {
        assertThat(PortalId.ZERO.toString()).isEqualTo(ZERO_UUID);
    }
}
