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

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.Collection;

public interface HttpExecutionContextInternal extends HttpExecutionContext {
    @Override
    HttpRequestInternal request();

    @Override
    HttpResponseInternal response();

    HttpExecutionContextInternal request(Request request);

    HttpExecutionContextInternal response(Response response);

    HttpExecutionContextInternal metrics(Metrics metrics);

    HttpExecutionContextInternal componentProvider(final ComponentProvider componentProvider);

    HttpExecutionContextInternal templateVariableProviders(final Collection<TemplateVariableProvider> templateVariableProviders);

    Collection<TemplateVariableProvider> templateVariableProviders();
}
