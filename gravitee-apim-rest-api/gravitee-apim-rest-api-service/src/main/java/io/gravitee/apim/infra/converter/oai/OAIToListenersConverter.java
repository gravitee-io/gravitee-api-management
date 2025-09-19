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
package io.gravitee.apim.infra.converter.oai;

import static java.util.Collections.singletonList;

import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.service.swagger.converter.extension.VirtualHost;
import io.gravitee.rest.api.service.swagger.converter.extension.XGraviteeIODefinition;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class OAIToListenersConverter {

    public static OAIToListenersConverter INSTANCE = new OAIToListenersConverter();
    private static final String DEFAULT_ENTRYPOINT_TYPE = "http-proxy";

    List<Listener> convert(XGraviteeIODefinition xGraviteeIODefinition, String apiName, String defaultEndpoint) {
        var listener = HttpListener.builder().entrypoints(singletonList(Entrypoint.builder().type(DEFAULT_ENTRYPOINT_TYPE).build()));
        var paths = Optional.ofNullable(xGraviteeIODefinition)
            .map(XGraviteeIODefinition::getVirtualHosts)
            .map(this::createPathsFromVirtualHosts)
            .orElseGet(() -> singletonList(createDefaultPath(apiName, defaultEndpoint)));

        return singletonList(listener.paths(paths).build());
    }

    private List<Path> createPathsFromVirtualHosts(List<VirtualHost> virtualHosts) {
        return virtualHosts
            .stream()
            .map(vHost ->
                Path.builder()
                    .host(vHost.getHost())
                    .path(vHost.getPath())
                    .overrideAccess(vHost.getOverrideEntrypoint() != null ? vHost.getOverrideEntrypoint() : false)
                    .build()
            )
            .collect(Collectors.toList());
    }

    private Path createDefaultPath(String apiName, String defaultEndpoint) {
        String contextPath = null;
        if (defaultEndpoint != null) {
            contextPath = URI.create(defaultEndpoint).getPath();
        }
        if (contextPath == null || contextPath.isEmpty() || contextPath.equals("/")) {
            contextPath = apiName.replaceAll("\\s+", "").toLowerCase();
        }
        return new Path(contextPath);
    }
}
