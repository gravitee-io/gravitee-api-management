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

import io.gravitee.gateway.policy.dummy.DummyPolicyConfiguration;
import io.gravitee.gateway.policy.impl.CachedPolicyConfigurationFactory;
import io.gravitee.policy.api.PolicyConfiguration;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CachedPolicyConfigurationFactoryTest {

    private PolicyConfigurationFactory policyConfigurationFactory;

    @Before
    public void setUp() {
        policyConfigurationFactory = new CachedPolicyConfigurationFactory();
    }

    @Test
    public void createPolicyConfigurationFromCache() {
        try (InputStream is = PolicyConfigurationFactoryTest.class.getResourceAsStream("policy-configuration-01.json")) {
            String configuration = IOUtils.toString(is, "UTF-8");
            DummyPolicyConfiguration policyConfiguration = policyConfigurationFactory.create(DummyPolicyConfiguration.class, configuration);
            DummyPolicyConfiguration policyConfiguration2 = policyConfigurationFactory.create(
                DummyPolicyConfiguration.class,
                configuration
            );

            Assert.assertNotNull(policyConfiguration);
            Assert.assertNotNull(policyConfiguration2);

            Assert.assertEquals(policyConfiguration, policyConfiguration2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createNullPolicyConfigurationFromCache() {
        try (InputStream is = PolicyConfigurationFactoryTest.class.getResourceAsStream("policy-configuration-01.json")) {
            String configuration = IOUtils.toString(is, "UTF-8");
            DummyPolicyConfiguration policyConfiguration = policyConfigurationFactory.create(DummyPolicyConfiguration.class, null);
            DummyPolicyConfiguration policyConfiguration2 = policyConfigurationFactory.create(
                DummyPolicyConfiguration.class,
                configuration
            );

            Assert.assertNull(policyConfiguration);
            Assert.assertNotNull(policyConfiguration2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createNullPolicyClassFromCache() {
        PolicyConfiguration policyConfiguration1 = policyConfigurationFactory.create(null, null);
        Assert.assertNull(policyConfiguration1);
    }
}
