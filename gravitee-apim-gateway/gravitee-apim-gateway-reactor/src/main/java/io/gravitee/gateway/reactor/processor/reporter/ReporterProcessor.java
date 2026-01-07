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
package io.gravitee.gateway.reactor.processor.reporter;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.reporter.api.http.Metrics;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ReporterProcessor extends AbstractProcessor<ExecutionContext> {

    private final ReporterService reporterService;

    public ReporterProcessor(final ReporterService reporterService) {
        this.reporterService = reporterService;
    }

    @Override
    public void handle(ExecutionContext context) {
        try {
            setQuota(context, context.request().metrics());

            reporterService.report(context.request().metrics());

            if (context.request().metrics().getLog() != null) {
                context.request().metrics().getLog().setApi(context.request().metrics().getApi());
                context.request().metrics().getLog().setApiName(context.request().metrics().getApiName());
                reporterService.report(context.request().metrics().getLog());
            }
        } catch (Exception ex) {
            log.error("An error occurs while reporting metrics", ex);
        }

        next.handle(context);
    }

    private static void setQuota(ExecutionContext ctx, Metrics metrics) {
        addLongMetric(metrics, ExecutionContext.ATTR_QUOTA_COUNT, ctx);
        addLongMetric(metrics, ExecutionContext.ATTR_QUOTA_LIMIT, ctx);
    }

    private static void addLongMetric(Metrics metrics, String key, ExecutionContext ctx) {
        Long value = getLongOrNull(ctx, key);
        if (value != null) {
            metrics.putAdditionalMetric("long_" + key.replace(ExecutionContext.ATTR_PREFIX, ""), value);
        }
    }

    private static Long getLongOrNull(ExecutionContext ctx, String key) {
        Object value = ctx.getAttribute(key);
        return (value instanceof Number) ? ((Number) value).longValue() : null;
    }
}
