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
package io.gravitee.gateway.reactive.reactor.v4.secrets;

import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsListener;
import io.gravitee.secrets.api.discovery.SecretRefsLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiV4DefinitionSecretRefsFinder extends AbstractV4APISecretRefFinder<Api> {

    public static final String RESPONSE_TEMPLATES_KIND = "response-templates";

    @Override
    public boolean canHandle(Object definition) {
        return canHandle(definition, Api.class);
    }

    @Override
    public DefinitionDescriptor toDefinitionDescriptor(Api definition, DefinitionMetadata metadata) {
        return new DefinitionDescriptor(new Definition("api-v4", definition.getId()), Optional.ofNullable(metadata.revision()));
    }

    @Override
    public void findSecretRefs(Api definition, DefinitionSecretRefsListener listener) {
        // listeners
        safeStream(definition.getListeners())
            .flatMap(l -> safeStream(l.getEntrypoints()))
            .forEach(entrypoint -> processEntrypoint(listener, entrypoint));

        // resources
        safeStream(definition.getResources()).forEach(resource -> processResource(listener, resource));

        // flows api and plan
        List<Flow> flows = safeList(definition.getPlans())
            .stream()
            .flatMap(p -> safeStream(p.getFlows()))
            .collect(Collectors.toCollection(ArrayList::new));
        flows.addAll(safeList(definition.getFlows()));
        Stream.concat(
            Stream.concat(
                flows.stream().flatMap(flow -> safeStream(flow.getRequest())),
                flows.stream().flatMap(flow -> safeStream(flow.getResponse()))
            ),
            Stream.concat(
                flows.stream().flatMap(flow -> safeStream(flow.getPublish())),
                flows.stream().flatMap(flow -> safeStream(flow.getSubscribe()))
            )
        ).forEach(step -> processStep(listener, step));

        safeStream(definition.getPlans()).forEach(plan -> processPlanConfiguration(listener, plan));

        // endpoint groups
        safeStream(definition.getEndpointGroups())
            .flatMap(endpointGroup -> {
                EndpointGroupServices services = endpointGroup.getServices();
                List<Service> list = new ArrayList<>();
                if (services.getDiscovery() != null) {
                    list.add(services.getDiscovery());
                }
                if (services.getHealthCheck() != null) {
                    list.add(services.getHealthCheck());
                }
                list
                    .stream()
                    .filter(Service::isEnabled)
                    .forEach(service -> processService(listener, service));
                return processEndpointGroup(listener, endpointGroup);
            })
            .forEach(endpoint -> {
                Optional.ofNullable(endpoint.getServices())
                    .map(EndpointServices::getHealthCheck)
                    .filter(Service::isEnabled)
                    .ifPresent(service -> processService(listener, service));
                processEndpoint(listener, endpoint);
            });

        // services
        Optional.ofNullable(definition.getServices())
            .map(ApiServices::getDynamicProperty)
            .ifPresent(dynamicProperty -> processService(listener, dynamicProperty));

        // response templates
        Map<String, Map<String, ResponseTemplate>> responseTemplates = definition.getResponseTemplates();
        safeKeySetStream(responseTemplates).forEach(key ->
            safeKeySetStream(responseTemplates.get(key)).forEach(contentType -> {
                ResponseTemplate responseTemplate = responseTemplates.get(key).get(contentType);
                Optional.ofNullable(responseTemplate.getBody()).ifPresent(body ->
                    listener.onCandidate(body, new SecretRefsLocation(RESPONSE_TEMPLATES_KIND, key), responseTemplate::setBody)
                );
                safeKeySetStream(responseTemplate.getHeaders()).forEach(headerName ->
                    listener.onCandidate(
                        responseTemplate.getHeaders().get(headerName),
                        new SecretRefsLocation(RESPONSE_TEMPLATES_KIND, key),
                        updated -> responseTemplate.getHeaders().put(headerName, updated)
                    )
                );
            })
        );
    }

    public <K, V> Stream<K> safeKeySetStream(Map<K, V> map) {
        return map == null ? Stream.empty() : map.keySet().stream();
    }
}
