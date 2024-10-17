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
package io.gravitee.gateway.reactive.handlers.api.el;

import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
import io.gravitee.gateway.reactor.ReactableApi;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@TemplateVariable(scopes = { TemplateVariableScope.API })
public class ApiTemplateVariableProvider implements TemplateVariableProvider {

    private final ApiVariables apiVariables;

    public ApiTemplateVariableProvider(final ReactableApi<? extends AbstractApi> api) {
        this.apiVariables = new ApiVariables(api);
    }

    @Override
    public void provide(TemplateContext templateContext) {
        templateContext.setVariable("api", apiVariables);
    }
}
