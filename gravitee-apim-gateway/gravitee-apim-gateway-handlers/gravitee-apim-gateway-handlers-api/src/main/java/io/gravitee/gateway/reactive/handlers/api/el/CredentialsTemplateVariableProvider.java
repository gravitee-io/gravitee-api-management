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

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
import io.gravitee.gateway.handlers.api.manager.CredentialResolver;

@TemplateVariable(scopes = { TemplateVariableScope.API })
public class CredentialsTemplateVariableProvider implements TemplateVariableProvider {

    static final String CREDENTIALS_VARIABLE = "credentials";

    private final String environmentId;
    private final String apiId;
    private final CredentialResolver credentialResolver;

    public CredentialsTemplateVariableProvider(String environmentId, String apiId, CredentialResolver credentialResolver) {
        this.environmentId = environmentId;
        this.apiId = apiId;
        this.credentialResolver = credentialResolver;
    }

    @Override
    public void provide(TemplateContext templateContext) {
        templateContext.setDeferredFunctionHolderVariable(
            CREDENTIALS_VARIABLE,
            new EvaluatedCredentialsMethods(environmentId, apiId, credentialResolver)
        );
    }
}
