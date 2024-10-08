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
package io.gravitee.gateway.policy.impl;

import static org.mockito.Mockito.mock;

import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.dummy.DummyPolicy;
import io.gravitee.policy.api.PolicyConfiguration;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpPolicyFactoryTest extends TestCase {

    @Mock
    private PolicyPluginFactory policyPluginFactory;

    private PolicyFactory cut;

    @Before
    public void setUp() {
        cut = new PolicyFactoryImpl(policyPluginFactory, new ExpressionLanguageStringConditionEvaluator());
    }

    @Test
    public void shouldCreateConditionalExecutablePolicyIfCondition() {
        final PolicyConfiguration policyConfiguration = mock(PolicyConfiguration.class);
        final Policy policy = cut.create(
            StreamType.ON_REQUEST,
            fakePolicyManifest(),
            policyConfiguration,
            fakePolicyMetadata("execution-condition")
        );

        assertTrue(policy instanceof ConditionalExecutablePolicy);
    }

    @Test
    public void shouldNotCreateConditionalExecutablePolicyIfNullCondition() {
        final PolicyConfiguration policyConfiguration = mock(PolicyConfiguration.class);
        final Policy policy = cut.create(StreamType.ON_REQUEST, fakePolicyManifest(), policyConfiguration, fakePolicyMetadata(null));

        assertTrue(policy instanceof ExecutablePolicy);
        assertFalse(policy instanceof ConditionalExecutablePolicy);
    }

    @Test
    public void shouldNotCreateConditionalExecutablePolicyIfEmptyCondition() {
        final PolicyConfiguration policyConfiguration = mock(PolicyConfiguration.class);
        final Policy policy = cut.create(StreamType.ON_REQUEST, fakePolicyManifest(), policyConfiguration, fakePolicyMetadata(""));

        assertTrue(policy instanceof ExecutablePolicy);
        assertFalse(policy instanceof ConditionalExecutablePolicy);
    }

    private PolicyManifest fakePolicyManifest() {
        return new PolicyManifestBuilder().setId("dummy-policy").setPolicy(DummyPolicy.class).setMethods(Map.of()).build();
    }

    private PolicyMetadata fakePolicyMetadata(String condition) {
        return condition != null ? new PolicyMetadata("dummy-policy", "{}", condition) : new PolicyMetadata("dummy-policy", "{}");
    }
}
