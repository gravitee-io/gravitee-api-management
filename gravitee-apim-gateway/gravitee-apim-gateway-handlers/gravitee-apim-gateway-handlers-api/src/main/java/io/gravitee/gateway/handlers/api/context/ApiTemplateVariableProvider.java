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
package io.gravitee.gateway.handlers.api.context;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
import io.gravitee.gateway.handlers.api.definition.Api;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@TemplateVariable(scopes = { TemplateVariableScope.API })
public class ApiTemplateVariableProvider implements TemplateVariableProvider {

    private final Api api;

    private final ApiProperties apiProperties;

    public ApiTemplateVariableProvider(final Api api) {
        this.api = api;
        this.apiProperties = new ApiProperties(api);
    }

    @Override
    public void provide(TemplateContext templateContext) {
        // Keep this variable for backward compatibility
        if (api.getProperties() != null) {
            templateContext.setVariable("properties", api.getProperties().getValues());
        }

        templateContext.setVariable("api", apiProperties);
    }
}
