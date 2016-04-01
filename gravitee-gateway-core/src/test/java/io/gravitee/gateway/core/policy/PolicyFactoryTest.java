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
package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.core.policy.impl.PolicyFactoryImpl;
import io.gravitee.policy.api.PolicyConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;


/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyFactoryTest {

    private PolicyFactory policyFactory;

    private PolicyConfigurationFactory policyConfigurationFactory;

    @Before
    public void setUp() {
        policyFactory = new PolicyFactoryImpl();
        policyConfigurationFactory = mock(PolicyConfigurationFactory.class);
        ((PolicyFactoryImpl) policyFactory).setPolicyConfigurationFactory(policyConfigurationFactory);
    }

    @Test
    public void createPolicyWithConfigurationAndWithoutConfigurationData() {
        PolicyClassDefinition definition = getPolicyDefinitionWithConfiguration();
        Object policy = policyFactory.create(definition, null);

        verify(policyConfigurationFactory, never()).create(any(), anyString());
        Assert.assertNull(policy);
    }

    @Test
    public void createPolicyWithoutConfigurationAndWithoutConfigurationData() {
        PolicyClassDefinition definition = getPolicyDefinitionWithoutConfiguration();
        Object policy = policyFactory.create(definition, null);

        verify(policyConfigurationFactory, never()).create(any(), anyString());
        Assert.assertNotNull(policy);
    }

    @Test
    public void createPolicyWithConfigurationAndConfigurationData() {
        PolicyClassDefinition definition = getPolicyDefinitionWithConfiguration();
        Object policy = policyFactory.create(definition, "{}");

        verify(policyConfigurationFactory, times(1)).create(any(), anyString());
        Assert.assertNotNull(policy);
    }

    @Test
    public void createPolicyWithoutConfigurationAndWithConfigurationData() {
        PolicyClassDefinition definition = getPolicyDefinitionWithoutConfiguration();
        Object policy = policyFactory.create(definition, "{}");

        verify(policyConfigurationFactory, never()).create(any(), anyString());
        Assert.assertNotNull(policy);
    }

    private PolicyClassDefinition getPolicyDefinitionWithConfiguration() {
        return new PolicyClassDefinition() {
            @Override
            public String id() {
                return null;
            }

            @Override
            public Class<?> policy() {
                return DummyPolicy.class;
            }

            @Override
            public Class<? extends PolicyConfiguration> configuration() {
                return DummyPolicyConfiguration.class;
            }

            @Override
            public Method onRequestMethod() {
                return null;
            }

            @Override
            public Method onRequestContentMethod() {
                return null;
            }

            @Override
            public Method onResponseMethod() {
                return null;
            }

            @Override
            public Method onResponseContentMethod() {
                return null;
            }
        };
    }

    private PolicyClassDefinition getPolicyDefinitionWithoutConfiguration() {
        return new PolicyClassDefinition() {
            @Override
            public String id() {
                return null;
            }

            @Override
            public Class<?> policy() {
                return DummyPolicy.class;
            }

            @Override
            public Class<? extends PolicyConfiguration> configuration() {
                return null;
            }

            @Override
            public Method onRequestMethod() {
                return null;
            }

            @Override
            public Method onRequestContentMethod() {
                return null;
            }

            @Override
            public Method onResponseMethod() {
                return null;
            }

            @Override
            public Method onResponseContentMethod() {
                return null;
            }
        };
    }

    class DummyPolicyConfiguration implements PolicyConfiguration {

    }
}
