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
package io.gravitee.gateway.reactor.handler.context;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.core.component.ComponentProvider;
import java.util.List;

/**
 * The default ExecutionContextFactory for gateway V3.
 *
 * {@inheritDoc}
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultV3ExecutionContextFactory implements V3ExecutionContextFactory {

    private final List<TemplateVariableProvider> providers;

    private final ComponentProvider componentProvider;

    public DefaultV3ExecutionContextFactory(ComponentProvider componentProvider, List<TemplateVariableProvider> providers) {
        this.componentProvider = componentProvider;
        this.providers = providers;
    }

    @Override
    public ExecutionContext create(ExecutionContext wrapped) {
        ReactableExecutionContext context = new ReactableExecutionContext((MutableExecutionContext) wrapped, componentProvider);
        context.setProviders(providers);
        return context;
    }
}
