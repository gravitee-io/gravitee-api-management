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
package io.gravitee.gateway.core.plugin;

import io.gravitee.gateway.core.plugin.impl.ClassLoaderFactoryImpl;
import io.gravitee.gateway.core.plugin.impl.PluginManagerImpl;
import io.gravitee.gateway.core.policy.impl.PolicyManagerImpl;
import io.gravitee.gateway.core.reporter.impl.ReporterManagerImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PluginManagerTest {

    private ClassLoaderFactory classLoaderFactory;
    private Collection<PluginHandler> pluginHandlers;

    @Before
    public void setUp() {
        classLoaderFactory = new ClassLoaderFactoryImpl();

        pluginHandlers = new ArrayList<>();
        pluginHandlers.add(new PolicyManagerImpl());
        pluginHandlers.add(new ReporterManagerImpl());
    }

    @Test(expected = RuntimeException.class)
    public void initWithInvalidWorkspace() {
        PluginManagerImpl pluginManager = new PluginManagerImpl();
        pluginManager.init();
    }

    @Test(expected = RuntimeException.class)
    public void initWithInexistantWorkspace() {
        PluginManagerImpl pluginManager = new PluginManagerImpl(
                "/io/gravitee/gateway/core/plugin/invalid/");
        pluginManager.init();
    }

    @Test
    public void initWithEmptyWorkspace() {
        URL dir = PluginManagerTest.class.getResource("/io/gravitee/gateway/core/plugin/empty-workspace/");
        PluginManagerImpl pluginManager = new PluginManagerImpl(dir.getPath());
        pluginManager.init();

        Assert.assertTrue(pluginManager.getPlugins().isEmpty());
    }

    @Test
    public void initTwiceWorkspace() {
        URL dir = PluginManagerTest.class.getResource("/io/gravitee/gateway/core/plugin/workspace/");
        PluginManagerImpl pluginManager = Mockito.spy(new PluginManagerImpl(dir.getPath()));
        pluginManager.setClassLoaderFactory(classLoaderFactory);
        pluginManager.setPluginHandlers(pluginHandlers);

        pluginManager.init();
        verify(pluginManager, atMost(1)).initializeFromWorkspace();

        pluginManager.init();
        verify(pluginManager, atMost(1)).initializeFromWorkspace();
    }

    @Test
    public void initWithWorkspace_noJar() {
        URL dir = PluginManagerTest.class.getResource("/io/gravitee/gateway/core/plugin/invalid-workspace-nojar/");
        PluginManagerImpl pluginManager = new PluginManagerImpl(dir.getPath());
        pluginManager.init();

        Assert.assertTrue(pluginManager.getPlugins().isEmpty());
    }

    @Test
    public void initWithValidWorkspace_onePolicyDefinition() {
        URL dir = PluginManagerTest.class.getResource("/io/gravitee/gateway/core/plugin/workspace/");
        PluginManagerImpl pluginManager = new PluginManagerImpl(dir.getPath());
        pluginManager.setClassLoaderFactory(classLoaderFactory);
        pluginManager.setPluginHandlers(pluginHandlers);
        pluginManager.init();

        Assert.assertEquals(1, pluginManager.getPlugins().size());
    }

    @Test
    public void initWithValidWorkspace_checkPluginDescriptor() {
        URL dir = PluginManagerTest.class.getResource("/io/gravitee/gateway/core/plugin/workspace/");
        PluginManagerImpl pluginManager = new PluginManagerImpl(dir.getPath());
        pluginManager.setClassLoaderFactory(classLoaderFactory);
        pluginManager.setPluginHandlers(pluginHandlers);
        pluginManager.init();

        Assert.assertEquals(1, pluginManager.getPlugins().size());

        Plugin plugin = pluginManager.getPlugins().iterator().next();
        Assert.assertEquals("my-policy", plugin.id());
        Assert.assertEquals("my.project.gravitee.policies.MyPolicy", plugin.clazz().getName());
    }
}
