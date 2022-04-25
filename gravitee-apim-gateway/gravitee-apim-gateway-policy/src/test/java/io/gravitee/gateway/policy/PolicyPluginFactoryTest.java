/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.policy;

import static org.mockito.Mockito.mock;

import io.gravitee.gateway.policy.dummy.DummyPolicy;
import io.gravitee.gateway.policy.dummy.DummyPolicyConfiguration;
import io.gravitee.gateway.policy.dummy.DummyPolicyWithConfig;
import io.gravitee.gateway.policy.dummy.DummyReactivePolicy;
import io.gravitee.gateway.policy.dummy.DummyReactivePolicyWithConfig;
import io.gravitee.gateway.policy.impl.PolicyPluginFactoryImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyPluginFactoryTest {

    private PolicyPluginFactory policyFactory;

    @Before
    public void setUp() {
        policyFactory = new PolicyPluginFactoryImpl();
    }

    @Test
    public void createPolicyWithConfigurationAndWithoutConfigurationData() {
        Object policy = policyFactory.create(DummyPolicyWithConfig.class, null);

        Assert.assertNotNull(policy);
    }

    @Test
    public void createPolicyWithConfigurationAndWithConfigurationData() {
        Object policy = policyFactory.create(DummyPolicyWithConfig.class, mock(DummyPolicyConfiguration.class));

        Assert.assertNotNull(policy);
    }

    @Test
    public void createPolicyWithoutConfigurationAndWithoutConfigurationData() {
        Object policy = policyFactory.create(DummyPolicy.class, null);

        Assert.assertNotNull(policy);
    }

    @Test
    public void createReactivePolicyWithConfigurationAndWithoutConfigurationData() {
        Object policy = policyFactory.create(DummyReactivePolicyWithConfig.class, null);

        Assert.assertNotNull(policy);
    }

    @Test
    public void createReactivePolicyWithConfigurationAndWithConfigurationData() {
        Object policy = policyFactory.create(DummyReactivePolicyWithConfig.class, mock(DummyPolicyConfiguration.class));

        Assert.assertNotNull(policy);
    }

    @Test
    public void createReactivePolicyWithoutConfigurationAndWithoutConfigurationData() {
        Object policy = policyFactory.create(DummyReactivePolicy.class, null);

        Assert.assertNotNull(policy);
    }
}
