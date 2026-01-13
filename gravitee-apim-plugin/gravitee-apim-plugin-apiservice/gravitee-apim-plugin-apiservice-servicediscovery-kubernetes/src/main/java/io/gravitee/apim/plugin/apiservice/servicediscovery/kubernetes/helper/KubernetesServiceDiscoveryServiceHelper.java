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

public final class KubernetesServiceDiscoveryServiceHelper {

    private static final String DEFAULT_NAMESPACE = "default";

    private KubernetesServiceDiscoveryServiceHelper() {}

    public static String resolveNamespace(KubernetesServiceDiscoveryServiceConfiguration configuration) {
        if (configuration.getNamespace() != null && !configuration.getNamespace().isBlank()) {
            return configuration.getNamespace();
        }
        String namespace = KubernetesConfig.getInstance().getCurrentNamespace();
        return namespace == null || namespace.isBlank() ? DEFAULT_NAMESPACE : namespace;
    }
}
