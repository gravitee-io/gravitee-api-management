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
package io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.Service;
import java.util.Map;

public class HttpProxyEndpointConfigurationFactory implements EndpointConfigurationFactory {

    public static final String ENDPOINT_TYPE = "http-proxy";

    @Override
    public String buildConfiguration(Service service) {
        var node = new JsonObject(Map.of("target", buildTargetUrl(service)));
        return node.toString();
    }

    private String buildTargetUrl(io.vertx.ext.consul.Service service) {
        var sslMetadata = (service.getMeta() != null) ? service.getMeta().get(CONSUL_METADATA_SSL) : null;
        var pathMetadata = (service.getMeta() != null) ? service.getMeta().get(CONSUL_METADATA_PATH) : null;

        var scheme = Boolean.parseBoolean(sslMetadata) ? "https" : "http";
        var host = service.getAddress() == null || service.getAddress().trim().isBlank() ? service.getNodeAddress() : service.getAddress();
        var port = (service.getPort() > 0 ? ":" + service.getPort() : "");
        var path = pathMetadata != null ? pathMetadata : "/";

        return scheme + "://" + host + port + path;
    }
}
