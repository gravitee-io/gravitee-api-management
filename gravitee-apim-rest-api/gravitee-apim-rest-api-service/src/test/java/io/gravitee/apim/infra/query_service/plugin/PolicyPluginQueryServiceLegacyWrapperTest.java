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
package io.gravitee.apim.infra.query_service.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyPluginQueryServiceLegacyWrapperTest {

    PolicyPluginService policyPluginService;
    PolicyPluginQueryServiceLegacyWrapper service;

    @BeforeEach
    void setup() {
        policyPluginService = mock(PolicyPluginService.class);
        service = new PolicyPluginQueryServiceLegacyWrapper(policyPluginService);
    }

    @Test
    void findAll() {
        when(policyPluginService.findAll())
            .thenReturn(
                Set.of(
                    PolicyPluginEntity.builder().id("policy-1").name("Policy 1").build(),
                    PolicyPluginEntity.builder().id("policy-2").name("Policy 2").build()
                )
            );

        assertThat(service.findAll())
            .containsExactlyInAnyOrder(
                PolicyPlugin.builder().id("policy-1").name("Policy 1").build(),
                PolicyPlugin.builder().id("policy-2").name("Policy 2").build()
            );
    }
}
