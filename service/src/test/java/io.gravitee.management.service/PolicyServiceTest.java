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
package io.gravitee.management.service;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.service.impl.PolicyServiceImpl;
import io.gravitee.plugin.api.Plugin;
import io.gravitee.plugin.api.PluginManifest;
import io.gravitee.plugin.api.PluginRegistry;
import io.gravitee.plugin.api.PluginType;
import io.gravitee.repository.exceptions.TechnicalException;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceTest {

    private static final String POLICY_NAME = "myPolicy";

    @InjectMocks
    private PolicyService policyService = new PolicyServiceImpl();

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private Plugin plugin;

    @Test
    public void shouldFindAll() throws TechnicalException {
        final PluginManifest manifest = Mockito.mock(PluginManifest.class);
        when(manifest.name()).thenReturn(POLICY_NAME);
        when(plugin.manifest()).thenReturn(manifest);
        when(pluginRegistry.plugins(PluginType.POLICY)).thenReturn(asList(plugin));

        final Set<PolicyEntity> policies = policyService.findAll();

        assertNotNull(policies);
        assertEquals(POLICY_NAME, policies.iterator().next().getName());
    }

    @Test
    public void shouldNotFindAll() throws TechnicalException {
        when(pluginRegistry.plugins(PluginType.POLICY)).thenReturn(null);

        final Set<PolicyEntity> policies = policyService.findAll();

        assertNotNull(policies);
        assertTrue(policies.isEmpty());
    }
}
