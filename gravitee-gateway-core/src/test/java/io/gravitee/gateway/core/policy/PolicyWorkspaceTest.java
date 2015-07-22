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

import io.gravitee.gateway.core.policy.impl.ClassLoaderFactoryImpl;
import io.gravitee.gateway.core.policy.impl.PolicyWorkspaceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;

import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyWorkspaceTest {

    private ClassLoaderFactory classLoaderFactory = new ClassLoaderFactoryImpl();

    @Test(expected = RuntimeException.class)
    public void initWithInvalidWorkspace() {
        PolicyWorkspace policyWorkspace = new PolicyWorkspaceImpl();
        policyWorkspace.init();
    }

    @Test(expected = RuntimeException.class)
    public void initWithInexistantWorkspace() {
        PolicyWorkspace policyWorkspace = new PolicyWorkspaceImpl(
                "/io/gravitee/gateway/core/policy/invalid/");
        policyWorkspace.init();
    }

    @Test
    public void initWithEmptyWorkspace() {
        URL dir = PolicyWorkspaceTest.class.getResource("/io/gravitee/gateway/core/policy/empty-workspace/");
        PolicyWorkspaceImpl policyWorkspace = new PolicyWorkspaceImpl(dir.getPath());
        policyWorkspace.init();

        Assert.assertTrue(policyWorkspace.getPolicyDefinitions().isEmpty());
    }

    @Test
    public void initTwiceWorkspace() {
        URL dir = PolicyWorkspaceTest.class.getResource("/io/gravitee/gateway/core/policy/workspace/");
        PolicyWorkspaceImpl policyWorkspace = Mockito.spy(new PolicyWorkspaceImpl(dir.getPath()));
        policyWorkspace.setClassLoaderFactory(classLoaderFactory);

        policyWorkspace.init();
        verify(policyWorkspace, atMost(1)).initializeFromWorkspace();

        policyWorkspace.init();
        verify(policyWorkspace, atMost(1)).initializeFromWorkspace();
    }

    @Test
    public void initWithWorkspace_noJar() {
        URL dir = PolicyWorkspaceTest.class.getResource("/io/gravitee/gateway/core/policy/invalid-workspace-nojar/");
        PolicyWorkspace policyWorkspace = new PolicyWorkspaceImpl(dir.getPath());
        policyWorkspace.init();

        Assert.assertTrue(policyWorkspace.getPolicyDefinitions().isEmpty());
    }

    @Test
    public void initWithValidWorkspace_onePolicyDefinition() {
        URL dir = PolicyWorkspaceTest.class.getResource("/io/gravitee/gateway/core/policy/workspace/");
        PolicyWorkspaceImpl policyWorkspace = new PolicyWorkspaceImpl(dir.getPath());
        policyWorkspace.setClassLoaderFactory(classLoaderFactory);
        policyWorkspace.init();

        Assert.assertTrue(policyWorkspace.getPolicyDefinitions().size() == 1);
    }

    @Test
    public void initWithValidWorkspace_checkPolicyDefinition() {
        URL dir = PolicyWorkspaceTest.class.getResource("/io/gravitee/gateway/core/policy/workspace/");
        PolicyWorkspaceImpl policyWorkspace = new PolicyWorkspaceImpl(dir.getPath());
        policyWorkspace.setClassLoaderFactory(classLoaderFactory);
        policyWorkspace.init();

        Assert.assertTrue(policyWorkspace.getPolicyDefinitions().size() == 1);

        PolicyDefinition definition = policyWorkspace.getPolicyDefinitions().iterator().next();
        Assert.assertEquals(definition.id(), "my-policy");
        Assert.assertEquals(definition.version(), "1.0.0-SNAPSHOT");
        Assert.assertEquals(definition.policy().getName(), "my.project.gravitee.policies.MyPolicy");
    }
}
