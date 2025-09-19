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

import static io.gravitee.secrets.api.discovery.SecretRefsLocation.PLUGIN_KIND;

import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpoint;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpointGroup;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.entrypoint.AbstractEntrypoint;
import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsListener;
import io.gravitee.secrets.api.discovery.SecretRefsLocation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractV4APISecretRefFinder<D> implements DefinitionSecretRefsFinder<D> {

    protected <T> List<T> safeList(List<T> col) {
        return col == null ? List.of() : col;
    }

    protected <T> Stream<T> safeStream(Collection<T> col) {
        return col == null ? Stream.empty() : col.stream();
    }

    protected boolean canHandle(Object definition, Class<D> clazz) {
        return definition != null && clazz.isAssignableFrom(definition.getClass());
    }

    protected void processEntrypoint(DefinitionSecretRefsListener listener, AbstractEntrypoint entrypoint) {
        listener.onCandidate(
            entrypoint.getConfiguration(),
            new SecretRefsLocation(PLUGIN_KIND, entrypoint.getType()),
            entrypoint::setConfiguration
        );
    }

    protected void processPlanConfiguration(DefinitionSecretRefsListener listener, AbstractPlan plan) {
        Optional.ofNullable(plan.getSecurity()).ifPresent(security ->
            listener.onCandidate(
                security.getConfiguration(),
                new SecretRefsLocation(PLUGIN_KIND, security.getType()),
                security::setConfiguration
            )
        );
    }

    protected void processStep(DefinitionSecretRefsListener listener, Step step) {
        listener.onCandidate(step.getConfiguration(), new SecretRefsLocation(PLUGIN_KIND, step.getPolicy()), step::setConfiguration);
    }

    protected <T extends AbstractEndpoint> Stream<T> processEndpointGroup(
        DefinitionSecretRefsListener listener,
        AbstractEndpointGroup<T> endpointGroup
    ) {
        Optional.ofNullable(endpointGroup.getSharedConfiguration()).ifPresent(payload ->
            listener.onCandidate(
                payload,
                new SecretRefsLocation(PLUGIN_KIND, endpointGroup.getType()),
                endpointGroup::setSharedConfiguration
            )
        );
        return safeStream(endpointGroup.getEndpoints());
    }

    protected static void processResource(DefinitionSecretRefsListener listener, Resource resource) {
        listener.onCandidate(
            resource.getConfiguration(),
            new SecretRefsLocation(PLUGIN_KIND, resource.getType()),
            resource::setConfiguration
        );
    }

    protected void processEndpoint(DefinitionSecretRefsListener listener, AbstractEndpoint endpoint) {
        listener.onCandidate(
            endpoint.getConfiguration(),
            new SecretRefsLocation(PLUGIN_KIND, endpoint.getType()),
            endpoint::setConfiguration
        );
        listener.onCandidate(
            endpoint.getSharedConfigurationOverride(),
            new SecretRefsLocation(PLUGIN_KIND, endpoint.getType()),
            endpoint::setSharedConfigurationOverride
        );
    }

    protected void processService(DefinitionSecretRefsListener listener, Service service) {
        listener.onCandidate(service.getConfiguration(), new SecretRefsLocation(PLUGIN_KIND, service.getType()), service::setConfiguration);
    }
}
