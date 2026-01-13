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
package io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.helper;

import io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.KubernetesServiceDiscoveryServiceConfiguration;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KubernetesServiceDiscoveryServiceHelperTest {

    private final KubernetesConfig kubernetesConfig = KubernetesConfig.getInstance();
    private String previousNamespace;
    private static final String CUSTOM_NAMESPACE = "custom";
    private static final String FALLBACK_NAMESPACE = "fallback";
    private static final String DEFAULT_NAMESPACE = "default";

    @AfterEach
    void tearDown() {
        kubernetesConfig.setCurrentNamespace(previousNamespace);
    }

    @Test
    void shouldPreferConfiguredNamespace() {
        previousNamespace = kubernetesConfig.getCurrentNamespace();
        kubernetesConfig.setCurrentNamespace(FALLBACK_NAMESPACE);

        KubernetesServiceDiscoveryServiceConfiguration config = new KubernetesServiceDiscoveryServiceConfiguration();
        config.setNamespace(CUSTOM_NAMESPACE);

        Assertions.assertEquals(CUSTOM_NAMESPACE, KubernetesServiceDiscoveryServiceHelper.resolveNamespace(config));
    }

    @Test
    void shouldDefaultNamespaceWhenUnavailable() {
        previousNamespace = kubernetesConfig.getCurrentNamespace();
        kubernetesConfig.setCurrentNamespace("");

        KubernetesServiceDiscoveryServiceConfiguration config = new KubernetesServiceDiscoveryServiceConfiguration();

        Assertions.assertEquals(DEFAULT_NAMESPACE, KubernetesServiceDiscoveryServiceHelper.resolveNamespace(config));
    }
}
