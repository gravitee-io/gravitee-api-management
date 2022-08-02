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
package io.gravitee.gateway.jupiter.reactor.handler.context;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.el.EvaluableRequest;
import io.gravitee.gateway.jupiter.api.el.EvaluableResponse;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import java.util.Collection;

/**
 * Default implementation of {@link RequestExecutionContext} to use when handling sync api requests.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultRequestExecutionContext
    extends AbstractExecutionContext<MutableRequest, MutableResponse>
    implements MutableRequestExecutionContext {

    private TemplateEngine templateEngine;
    private Collection<TemplateVariableProvider> templateVariableProviders;

    public DefaultRequestExecutionContext(final MutableRequest request, final MutableResponse response) {
        super(request, response);
    }

    public DefaultRequestExecutionContext templateVariableProviders(final Collection<TemplateVariableProvider> templateVariableProviders) {
        this.templateVariableProviders = templateVariableProviders;
        return this;
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        if (templateEngine == null) {
            templateEngine = TemplateEngine.templateEngine();

            final TemplateContext templateContext = templateEngine.getTemplateContext();
            final EvaluableRequest evaluableRequest = new EvaluableRequest(request());
            final EvaluableResponse evaluableResponse = new EvaluableResponse(response());

            templateContext.setVariable(TEMPLATE_ATTRIBUTE_REQUEST, evaluableRequest);
            templateContext.setVariable(TEMPLATE_ATTRIBUTE_RESPONSE, evaluableResponse);
            templateContext.setVariable(TEMPLATE_ATTRIBUTE_CONTEXT, new EvaluableExecutionContext(this));

            if (templateVariableProviders != null) {
                templateVariableProviders.forEach(templateVariableProvider -> templateVariableProvider.provide(this));
            }
        }

        return templateEngine;
    }
}
