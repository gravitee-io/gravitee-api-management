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
package io.gravitee.gateway.handlers.sharedpolicygroup.reactor.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SuppressWarnings("deprecation")
class DefaultSharedPolicyGroupReactorFactoryTest {

    @ParameterizedTest
    @EnumSource(
        value = SharedPolicyGroup.Phase.class,
        names = { "REQUEST", "RESPONSE", "PUBLISH", "SUBSCRIBE", "MESSAGE_REQUEST", "MESSAGE_RESPONSE" }
    )
    void should_create_reactor_for_supported_phases(SharedPolicyGroup.Phase phase) {
        var cut = new DefaultSharedPolicyGroupReactorFactory(null, null, null, null, null, null);
        var reactableSharedPolicyGroup = ReactableSharedPolicyGroup.builder()
            .definition(SharedPolicyGroup.builder().phase(phase).build())
            .build();

        assertThat(cut.canCreate(reactableSharedPolicyGroup)).isTrue();
    }
}
