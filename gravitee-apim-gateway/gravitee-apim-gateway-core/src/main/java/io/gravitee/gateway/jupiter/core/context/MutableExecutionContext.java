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
package io.gravitee.gateway.jupiter.core.context;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import java.util.Collection;

public interface MutableExecutionContext extends ExecutionContext {
    @Override
    MutableRequest request();

    @Override
    MutableResponse response();

    MutableExecutionContext request(Request request);

    MutableExecutionContext response(Response response);

    MutableExecutionContext componentProvider(final ComponentProvider componentProvider);

    MutableExecutionContext templateVariableProviders(final Collection<TemplateVariableProvider> templateVariableProviders);

    Collection<TemplateVariableProvider> templateVariableProviders();
}
