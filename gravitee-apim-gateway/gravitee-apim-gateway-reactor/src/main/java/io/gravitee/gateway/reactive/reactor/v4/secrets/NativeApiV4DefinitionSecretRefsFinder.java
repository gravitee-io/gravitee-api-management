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

import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NativeApiV4DefinitionSecretRefsFinder extends AbstractV4APISecretRefFinder<NativeApi> {

    public static final String NATIVE_API_V4_DEFINITION_KIND = "native-api-v4";

    @Override
    public boolean canHandle(Object definition) {
        return canHandle(definition, NativeApi.class);
    }

    @Override
    public DefinitionDescriptor toDefinitionDescriptor(NativeApi definition, DefinitionMetadata metadata) {
        return new DefinitionDescriptor(
            new Definition(NATIVE_API_V4_DEFINITION_KIND, definition.getId()),
            Optional.ofNullable(metadata.revision())
        );
    }

    @Override
    public void findSecretRefs(NativeApi definition, DefinitionSecretRefsListener listener) {
        // listeners
        safeStream(definition.getListeners())
            .flatMap(l -> safeStream(l.getEntrypoints()))
            .forEach(entrypoint -> processEntrypoint(listener, entrypoint));

        // resources
        safeStream(definition.getResources()).forEach(resource -> processResource(listener, resource));

        // flows api and plan
        List<NativeFlow> flows = safeList(definition.getPlans())
            .stream()
            .flatMap(p -> safeStream(p.getFlows()))
            .collect(Collectors.toCollection(ArrayList::new));
        flows.addAll(safeList(definition.getFlows()));
        Stream.concat(
            Stream.concat(
                flows.stream().flatMap(flow -> safeStream(flow.getPublish())),
                flows.stream().flatMap(flow -> safeStream(flow.getSubscribe()))
            ),
            Stream.concat(
                flows.stream().flatMap(flow -> safeStream(flow.getEntrypointConnect())),
                flows.stream().flatMap(flow -> safeStream(flow.getInteract()))
            )
        ).forEach(step -> processStep(listener, step));

        safeStream(definition.getPlans()).forEach(plan -> processPlanConfiguration(listener, plan));

        // endpoint groups
        safeStream(definition.getEndpointGroups())
            .flatMap(endpointGroup -> processEndpointGroup(listener, endpointGroup))
            .forEach(endpoint -> processEndpoint(listener, endpoint));

        // services
        Optional.ofNullable(definition.getServices())
            .map(NativeApiServices::getDynamicProperty)
            .ifPresent(dynamicProperty -> processService(listener, dynamicProperty));
    }
}
