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

import io.gravitee.gateway.policy.impl.PolicyFactoryImpl;
import io.gravitee.policy.api.PolicyConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.Collections;

import static org.mockito.Mockito.*;


/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyFactoryTest {

    private PolicyFactory policyFactory;

    @Before
    public void setUp() {
        policyFactory = new PolicyFactoryImpl();
    }

    @Test
    public void createPolicyWithConfigurationAndWithoutConfigurationData() {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        when(policyDefinition.configuration()).then((Answer<Class>) invocationOnMock -> DummyPolicyConfiguration.class);
        Object policy = policyFactory.create(policyDefinition, null);

//        verify(policyConfigurationFactory, never()).create(any(), anyString());
        Assert.assertNotNull(policy);
    }

    @Test
    public void createPolicyWithoutConfigurationAndWithoutConfigurationData() {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Object policy = policyFactory.create(policyDefinition, null);

//        verify(policyConfigurationFactory, never()).create(any(), anyString());
        Assert.assertNotNull(policy);
    }

    @Test
    public void createPolicyWithConfigurationAndConfigurationData() {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        when(policyDefinition.configuration()).then((Answer<Class>) invocationOnMock -> DummyPolicyConfiguration.class);

        Object policy = policyFactory.create(policyDefinition, Collections.emptyMap());

//        verify(policyConfigurationFactory, times(1)).create(any(), anyString());
        Assert.assertNotNull(policy);
    }

    @Test
    public void createPolicyWithoutConfigurationAndWithConfigurationData() {
        PolicyMetadata policyDefinition = mock(PolicyMetadata.class);
        when(policyDefinition.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Object policy = policyFactory.create(policyDefinition, Collections.emptyMap());

//        verify(policyConfigurationFactory, never()).create(any(), anyString());
        Assert.assertNotNull(policy);
    }

    class DummyPolicyConfiguration implements PolicyConfiguration {

    }
}
