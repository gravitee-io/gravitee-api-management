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
package io.gravitee.gateway.handlers.api.secrets;

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactive.reactor.v4.secrets.AbstractV4APISecretRefFinder;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsListener;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds the secret references of an API Product: plan security, plan flows, product flows and product resources.
 */
public class ApiProductDefinitionSecretRefsFinder extends AbstractV4APISecretRefFinder<ReactableApiProduct> {

    public static final String API_PRODUCT_DEFINITION_KIND = "api-product";

    @Override
    public boolean canHandle(Object definition) {
        return canHandle(definition, ReactableApiProduct.class);
    }

    @Override
    public DefinitionDescriptor toDefinitionDescriptor(ReactableApiProduct definition, DefinitionMetadata metadata) {
        return new DefinitionDescriptor(
            new Definition(API_PRODUCT_DEFINITION_KIND, definition.getId()),
            Optional.ofNullable(metadata.revision())
        );
    }

    @Override
    public void findSecretRefs(ReactableApiProduct definition, DefinitionSecretRefsListener listener) {
        safeStream(definition.getPlans()).forEach(plan -> processPlanConfiguration(listener, plan));
        Stream.concat(safeStream(definition.getPlans()).flatMap(plan -> safeStream(plan.getFlows())), safeStream(definition.getFlows()))
            .flatMap(flow ->
                Stream.of(flow.getRequest(), flow.getResponse(), flow.getPublish(), flow.getSubscribe()).flatMap(this::safeStream)
            )
            .forEach(step -> processStep(listener, step));
        safeStream(definition.getResources()).forEach(resource -> processResource(listener, resource));
    }
}
