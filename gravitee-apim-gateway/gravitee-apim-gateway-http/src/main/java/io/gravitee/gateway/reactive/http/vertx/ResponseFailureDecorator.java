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
package io.gravitee.gateway.reactive.http.vertx;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.context.ComponentScope;
import io.gravitee.gateway.reactive.core.context.diagnostic.DiagnosticReportHelper;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import io.gravitee.reporter.api.v4.metric.Metrics;

/**
 * Decorates an in-flight response with a component-attributed failure without interrupting the dispatch. Uses
 * {@link DiagnosticReportHelper}, the same conversion path as {@code AbstractExecutionContext.interruptWith}, while
 * preserving the first failure already recorded on the request.
 */
final class ResponseFailureDecorator {

    private ResponseFailureDecorator() {}

    /**
     * Records the failure on both the structured diagnostic and legacy flat fields. No-op when metrics are not
     * available, the request has already ended, or another failure won the race.
     *
     * @return {@code true} when this failure was recorded
     */
    static boolean decorate(HttpBaseExecutionContext ctx, ExecutionFailure failure) {
        Metrics metrics = ctx.metrics();
        if (metrics == null || metrics.isRequestEnded() || metrics.getFailure() != null || metrics.getErrorKey() != null) {
            return false;
        }

        ComponentScope.ComponentEntry component = ComponentScope.peek((BaseExecutionContext) ctx);
        Diagnostic diagnostic = DiagnosticReportHelper.fromExecutionFailure(
            component,
            metrics.getErrorKey(),
            metrics.getErrorMessage(),
            failure
        );
        metrics.setFailure(diagnostic);
        // Legacy flat fields stay in sync for dashboards that do not consume structured diagnostics yet.
        metrics.setErrorKey(diagnostic.getKey());
        metrics.setErrorMessage(diagnostic.getMessage());
        return true;
    }
}
