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
package io.gravitee.rest.api.model.configuration.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author GraviteeSource Team
 */
class GroupMappingEntityTest {

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{#jsonPath(#profile, '$.groups')}",
            "{ #jsonPath(#profile, '$.groups')}",
            "{  #profile['groups']}",
            "{T(java.util.List).of('admin')}",
            "{('admin')}",
            "team-{#profile['team']}",
            " {#profile['groups']}",
        }
    )
    void should_detect_an_expression(String group) {
        assertThat(GroupMappingEntity.isExpression(group)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = {
            "3f0b5c2e-6a1d-4b8e-9c7f-2d4e6a8b0c1d", "platform-admin", "#profile", "{profile}", "{ admin}", "{Tech}", "{ Tech-leads}",
        }
    )
    void should_not_detect_an_expression(String group) {
        assertThat(GroupMappingEntity.isExpression(group)).isFalse();
    }
}
