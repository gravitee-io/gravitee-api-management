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

import io.gravitee.gateway.core.policy.impl.PolicyRegistryImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyRegistryTest {

    private PolicyRegistryImpl policyRegistry;

    @Mock
    private PolicyLoader policyLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        policyRegistry = new PolicyRegistryImpl();
        policyRegistry.setPolicyLoader(policyLoader);
    }

    @Test
    public void registerEmptyDescriptor() {
        Collection<PolicyDescriptor> descriptors = new ArrayList<>();
        when(policyLoader.load()).thenReturn(descriptors);

        policyRegistry.initialize();

        Collection<PolicyDefinition> policies = policyRegistry.policies();
        Assert.assertTrue(policies.isEmpty());
    }

    @Test
    public void registerInvalidDescriptor() {
        Collection<PolicyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(new PolicyDescriptor() {
            @Override
            public String id() {
                return null;
            }

            @Override
            public String name() {
                return null;
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String version() {
                return null;
            }

            @Override
            public String policy() {
                return null;
            }
        });
        when(policyLoader.load()).thenReturn(descriptors);

        policyRegistry.initialize();

        Collection<PolicyDefinition> policies = policyRegistry.policies();
        Assert.assertTrue(policies.isEmpty());
    }

}
