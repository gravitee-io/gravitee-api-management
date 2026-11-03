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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import org.junit.jupiter.api.Test;

class PolicyPluginAdapterTest {

    @Test
    void should_carry_supports_message_condition_to_the_core_model() {
        var entity = new PolicyPluginEntity();
        entity.setId("frame-policy");
        entity.setSupportsMessageCondition(false);

        assertThat(PolicyPluginAdapter.INSTANCE.map(entity).isSupportsMessageCondition()).isFalse();
    }

    @Test
    void should_keep_message_condition_supported_by_default() {
        var entity = new PolicyPluginEntity();
        entity.setId("message-policy");

        assertThat(PolicyPluginAdapter.INSTANCE.map(entity).isSupportsMessageCondition()).isTrue();
    }
}
