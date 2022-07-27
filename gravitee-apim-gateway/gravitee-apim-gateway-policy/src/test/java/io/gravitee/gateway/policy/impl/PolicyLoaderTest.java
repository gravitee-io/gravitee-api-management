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
package io.gravitee.gateway.policy.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.policy.PolicyDefinition;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.dummy.*;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.internal.PolicyClassLoaderFactoryImpl;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class PolicyLoaderTest {

    private PolicyLoader policyLoader;

    @BeforeEach
    public void init() {
        ConfigurablePluginManager policyPluginManager = mock(ConfigurablePluginManager.class);
        when(policyPluginManager.get("dummy-policy"))
            .thenReturn(new DummyPolicyPlugin("dummy-policy", DummyPolicy.class, DummyPolicyConfiguration.class, DummyPolicyContext.class));
        when(policyPluginManager.get("dummy-reactive"))
            .thenReturn(
                new DummyPolicyPlugin("dummy-reactive", DummyReactivePolicy.class, DummyPolicyConfiguration.class, DummyPolicyContext.class)
            );
        policyLoader =
            new PolicyLoader(
                new DefaultClassLoader(this.getClass().getClassLoader()),
                policyPluginManager,
                new PolicyClassLoaderFactoryImpl(),
                new CustomComponentProvider()
            );
    }

    @Test
    public void shouldReturnEmptyLoadedPoliciesFromEmptyDependencies() {
        Map<String, PolicyManifest> load = policyLoader.load(Set.of());
        assertTrue(load.isEmpty());
    }

    @Test
    public void shouldReturnLoadedPolicies() {
        PolicyDefinition dummyPolicy = new PolicyDefinition("dummy-policy", "{}");
        PolicyDefinition dummyReactivePolicy = new PolicyDefinition("dummy-reactive", "{}");
        Map<String, PolicyManifest> load = policyLoader.load(Set.of(dummyPolicy, dummyReactivePolicy));
        assertThat(load.size()).isEqualTo(2);
        assertThat(load.containsKey("dummy-policy")).isTrue();
        PolicyManifest dummyPolicyManifest = load.get("dummy-policy");
        assertThat(dummyPolicyManifest.id()).isEqualTo("dummy-policy");
        assertThat(dummyPolicyManifest.policy()).isEqualTo(DummyPolicy.class);
        assertThat(dummyPolicyManifest.configuration()).isEqualTo(DummyPolicyConfiguration.class);
        assertThat(dummyPolicyManifest.context()).isNotNull();
        assertThat(dummyPolicyManifest.context()).isInstanceOf(DummyPolicyContext.class);
        assertThat(dummyPolicyManifest.method(OnRequest.class)).isNotNull();
        assertThat(dummyPolicyManifest.method(OnRequestContent.class)).isNotNull();
        assertThat(dummyPolicyManifest.method(OnResponse.class)).isNotNull();
        assertThat(dummyPolicyManifest.method(OnResponseContent.class)).isNotNull();
        assertThat(load.containsKey("dummy-reactive")).isTrue();
        PolicyManifest reactivePolicyManifest = load.get("dummy-reactive");
        assertThat(reactivePolicyManifest.id()).isEqualTo("dummy-reactive");
        assertThat(reactivePolicyManifest.policy()).isEqualTo(DummyReactivePolicy.class);
        assertThat(reactivePolicyManifest.configuration()).isEqualTo(DummyPolicyConfiguration.class);
        assertThat(reactivePolicyManifest.context()).isNotNull();
        assertThat(reactivePolicyManifest.context()).isInstanceOf(DummyPolicyContext.class);
        assertThat(reactivePolicyManifest.method(OnRequest.class)).isNull();
        assertThat(reactivePolicyManifest.method(OnRequestContent.class)).isNull();
        assertThat(reactivePolicyManifest.method(OnResponse.class)).isNull();
        assertThat(reactivePolicyManifest.method(OnResponseContent.class)).isNull();
    }
}
