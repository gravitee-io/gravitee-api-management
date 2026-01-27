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
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.logging.AbstractBaseExecutionContextAwareLogger;
import io.gravitee.gateway.reactive.api.logging.ExecutionContextLazyLogger;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.node.logging.LogEntry;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Default implementation of {@link io.gravitee.gateway.reactive.api.context.ExecutionContext} to use when handling requests.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultExecutionContext extends AbstractExecutionContext<MutableRequest, MutableResponse> implements MutableExecutionContext {

    private static final Set<Class<?>> CONTEXT_CLASSES;

    static {
        Set<Class<?>> classes = new HashSet<>();
        collectParentClasses(DefaultExecutionContext.class, classes);
        CONTEXT_CLASSES = Set.copyOf(classes);
    }

    private ConcurrentHashMap<Logger, Logger> loggers;

    private Set<LogEntry<? extends HttpExecutionContextInternal>> logEntries;

    public DefaultExecutionContext(final MutableRequest request, final MutableResponse response) {
        super(request, response);
    }

    @Override
    public DefaultExecutionContext request(Request request) {
        return this;
    }

    @Override
    public DefaultExecutionContext response(Response response) {
        return this;
    }

    @Override
    public DefaultExecutionContext metrics(final Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    public DefaultExecutionContext tracer(final Tracer tracer) {
        this.tracer = tracer;
        return this;
    }

    @Override
    public long timestamp() {
        return this.request.timestamp();
    }

    public DefaultExecutionContext componentProvider(final ComponentProvider componentProvider) {
        this.componentProvider = componentProvider;
        return this;
    }

    public DefaultExecutionContext templateVariableProviders(final Collection<TemplateVariableProvider> templateVariableProviders) {
        this.templateVariableProviders = templateVariableProviders;
        return this;
    }

    public DefaultExecutionContext logEntries(Set<LogEntry<? extends HttpExecutionContextInternal>> logEntries) {
        this.logEntries = logEntries;
        return this;
    }

    @Override
    public Logger withLogger(Logger delegate) {
        if (loggers == null) {
            loggers = new ConcurrentHashMap<>();
        }

        return loggers.computeIfAbsent(delegate, logger ->
            ExecutionContextLazyLogger.lazy(logger, this, DefaultExecutionContextAwareLogger::new)
        );
    }

    /**
     * Collects all the parent classes and interfaces of the given class, adding them to the specified set.
     * Avoids including duplicates and stops processing once the base class `BaseExecutionContext` is encountered.
     *
     * @param clazz the class whose parent classes and interfaces are to be collected
     * @param classes the set that will contain the collected parent classes and interfaces
     */
    private static void collectParentClasses(Class<?> clazz, Set<Class<?>> classes) {
        if (clazz == null) {
            return;
        }
        if (classes.add(clazz) && !BaseExecutionContext.class.equals(clazz)) {
            for (Class<?> inter : clazz.getInterfaces()) {
                collectParentClasses(inter, classes);
            }
            collectParentClasses(clazz.getSuperclass(), classes);
        }
    }

    private class DefaultExecutionContextAwareLogger extends AbstractBaseExecutionContextAwareLogger<DefaultExecutionContext> {

        public DefaultExecutionContextAwareLogger(DefaultExecutionContext context, Logger logger) {
            super(context, logger);
        }

        @Override
        protected void registerLogEntries(Set<LogEntry<?>> entries) {
            super.registerLogEntries(entries);
            if (logEntries != null) {
                entries.addAll(logEntries);
            }
        }

        /**
         * Registers log sources for the provided map. This includes the {@link DefaultExecutionContext}
         * and all parent classes defined in the static {@code CONTEXT_CLASSES} set.
         *
         * @param logSources the map where log sources are registered, mapping a class to an instance or context.
         */
        @Override
        protected void registerLogSources(Map<Class<?>, Object> logSources) {
            super.registerLogSources(logSources);
            logSources.putIfAbsent(DefaultExecutionContext.class, context);
            for (Class<?> clazz : CONTEXT_CLASSES) {
                logSources.putIfAbsent(clazz, context);
            }
        }
    }
}
