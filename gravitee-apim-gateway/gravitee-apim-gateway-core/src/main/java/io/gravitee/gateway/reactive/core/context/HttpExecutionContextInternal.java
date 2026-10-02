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
import io.gravitee.gateway.reactive.api.context.EntityResolver;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.node.logging.LogEntry;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface HttpExecutionContextInternal extends HttpExecutionContext {
    @Override
    HttpRequestInternal request();

    @Override
    HttpResponseInternal response();

    HttpExecutionContextInternal request(Request request);

    HttpExecutionContextInternal response(Response response);

    HttpExecutionContextInternal metrics(Metrics metrics);

    HttpExecutionContextInternal componentProvider(final ComponentProvider componentProvider);

    /**
     * Allows defining the TemplateVariableProviders to use for resolving variables in templates.
     * TemplateVariableProvider are used when initializing the TemplateEngine.
     * The TemplateVariableProvider <b>should not be changed after calling this method!</b
     *
     * @param templateVariableProviders collection of TemplateVariableProvider
     * @return the current instance of HttpExecutionContextInternal
     */
    HttpExecutionContextInternal templateVariableProviders(final Collection<TemplateVariableProvider> templateVariableProviders);

    HttpExecutionContextInternal tracer(Tracer tracer);

    HttpExecutionContextInternal entityResolver(EntityResolver entityResolver);

    /**
     * Get the action to be executed at the response phase for a given source.
     * @return the action to be executed at the response phase or <code>null</code> if no action has been registered for this source.
     */
    Map<BasePolicy, Function<HttpExecutionContext, Completable>> getOnResponseActions();

    /**
     * Takes the action registered for a source, so that it can be executed at most once however many times the
     * response actions are executed. They are executed both from the response phase and, for a request that
     * never reaches it, from the segment that always runs; whichever gets there first is the only one to run it.
     *
     * @return the action, or <code>null</code> when the source registered none or it has already been taken.
     */
    Function<HttpExecutionContext, Completable> removeOnResponseAction(BasePolicy source);

    /**
     * Sets the log entries for the current execution context.
     *
     * @param logEntries a set of log entries to be applied to the current execution context. Each log entry must be an instance
     *                   or a subclass of {@code LogEntry<? extends HttpExecutionContextInternal>}.
     * @return the current instance of {@code HttpExecutionContextInternal}, allowing method chaining.
     */
    HttpExecutionContextInternal logEntries(Set<LogEntry<? extends HttpExecutionContextInternal>> logEntries);
}
