/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.endpoint.factory.template;

import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.SpelTemplateEngine;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class TemplateAwareEndpointFactory<T extends io.gravitee.definition.model.Endpoint, S extends Endpoint> implements EndpointFactory<T, S> {

    protected final TemplateEngine templateEngine = TemplateEngine.templateEngine();

    @Override
    public S create(T endpoint, EndpointContext context) {
        if (context != null) {
            templateEngine.getTemplateContext().setVariable("properties", context.getProperties());
            endpoint = resolve(endpoint);
        }

        return this.create0(endpoint);
    }

    protected abstract T resolve(T endpoint);

    protected abstract S create0(T endpoint);
}
