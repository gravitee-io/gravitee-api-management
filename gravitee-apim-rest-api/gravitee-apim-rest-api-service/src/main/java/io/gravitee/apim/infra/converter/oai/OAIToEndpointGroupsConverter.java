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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OAIToEndpointGroupsConverter {

    public static OAIToEndpointGroupsConverter INSTANCE = new OAIToEndpointGroupsConverter();
    private static final String DEFAULT_ENDPOINT_GROUP_NAME = "default-group";
    private static final String DEFAULT_ENDPOINT_NAME = "default";
    private static final String DEFAULT_ENDPOINT_TYPE = "http-proxy";

    List<EndpointGroup> convert(List<Server> servers, List<String> serverUrls) {
        if (CollectionUtils.isEmpty(servers)) {
            return Collections.emptyList();
        }

        return singletonList(
            EndpointGroup.builder()
                .name(DEFAULT_ENDPOINT_GROUP_NAME)
                .type("http-proxy")
                .endpoints(
                    serverUrls
                        .stream()
                        .map(url -> {
                            var configurationNode = new ObjectMapper().createObjectNode();
                            configurationNode.put("target", url);
                            return Endpoint.builder()
                                .name(serverUrls.size() == 1 ? DEFAULT_ENDPOINT_NAME : "server" + (serverUrls.indexOf(url) + 1))
                                .configuration(configurationNode.toString())
                                .weight(1)
                                .inheritConfiguration(true)
                                .type(DEFAULT_ENDPOINT_TYPE)
                                .build();
                        })
                        .collect(Collectors.toList())
                )
                .build()
        );
    }
}
