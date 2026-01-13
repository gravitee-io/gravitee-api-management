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
package io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.factory;

import io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.KubernetesServiceDiscoveryServiceConfiguration;
import io.gravitee.kubernetes.client.model.v1.EndpointAddress;
import io.vertx.core.json.JsonObject;
import java.util.Map;

public class HttpProxyEndpointConfigurationFactory implements EndpointConfigurationFactory {

    public static final String ENDPOINT_TYPE = "http-proxy";
    private static final String DEFAULT_SCHEME = "http";
    private static final String DEFAULT_PATH = "/";

    @Override
    public String buildConfiguration(EndpointAddress address, int port, KubernetesServiceDiscoveryServiceConfiguration configuration) {
        var node = new JsonObject(Map.of("target", buildTargetUrl(address, port, configuration)));
        return node.toString();
    }

    private String buildTargetUrl(EndpointAddress address, int port, KubernetesServiceDiscoveryServiceConfiguration configuration) {
        var scheme = normalizeValue(configuration.getScheme(), DEFAULT_SCHEME);
        var path = normalizeValue(configuration.getPath(), DEFAULT_PATH);
        var portSuffix = port > 0 ? ":" + port : "";
        return scheme + "://" + address.getIp() + portSuffix + path;
    }

    private String normalizeValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
