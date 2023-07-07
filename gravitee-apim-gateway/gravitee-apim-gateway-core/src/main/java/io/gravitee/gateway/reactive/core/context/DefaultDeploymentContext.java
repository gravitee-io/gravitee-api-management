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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import java.util.Collection;

/**
 * Default implementation of {@link DeploymentContext} to use when needing to access deployment context (ex: api) to start services, connectors...
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultDeploymentContext implements DeploymentContext {

    protected ComponentProvider componentProvider;
    protected TemplateEngine templateEngine;
    protected Collection<TemplateVariableProvider> templateVariableProviders;

    public DefaultDeploymentContext componentProvider(final ComponentProvider componentProvider) {
        this.componentProvider = componentProvider;
        return this;
    }

    public DefaultDeploymentContext templateVariableProviders(final Collection<TemplateVariableProvider> templateVariableProviders) {
        this.templateVariableProviders = templateVariableProviders;
        return this;
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return componentProvider.getComponent(componentClass);
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        if (templateEngine == null) {
            templateEngine = TemplateEngine.templateEngine();
            prepareTemplateEngine(templateEngine);
        }

        return templateEngine;
    }

    private void prepareTemplateEngine(final TemplateEngine templateEngine) {
        final TemplateContext templateContext = templateEngine.getTemplateContext();

        if (templateVariableProviders != null) {
            templateVariableProviders.forEach(templateVariableProvider -> templateVariableProvider.provide(templateContext));
        }
    }
}
