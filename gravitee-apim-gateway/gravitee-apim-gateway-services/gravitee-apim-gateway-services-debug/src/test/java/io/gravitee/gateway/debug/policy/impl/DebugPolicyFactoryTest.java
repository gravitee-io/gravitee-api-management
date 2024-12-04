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
package io.gravitee.gateway.debug.policy.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.debug.policy.condition.DebugExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.HttpConditionalPolicy;
import io.gravitee.node.api.configuration.Configuration;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DebugPolicyFactoryTest {

    @Mock
    private PolicyPluginFactory mockPolicyPluginFactory;

    @Mock
    private ExpressionLanguageConditionFilter<HttpConditionalPolicy> mockFilter;

    @Mock
    private Configuration configuration;

    @InjectMocks
    private DebugPolicyFactory debugPolicyFactory;

    @Test
    public void testConstructor_ShouldInstantiateCorrectly() {
        DebugPolicyFactory debugPolicyFactory = new DebugPolicyFactory(
            configuration,
            mockPolicyPluginFactory,
            new DebugExpressionLanguageConditionFilter()
        );
        // assert that the DebugPolicyFactory object is created successfully
        assertNotNull(debugPolicyFactory, "DebugPolicyFactory should be instantiated correctly");
    }
}
