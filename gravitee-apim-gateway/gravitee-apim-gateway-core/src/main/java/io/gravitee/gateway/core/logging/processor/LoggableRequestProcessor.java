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
package io.gravitee.gateway.core.logging.processor;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.core.logging.LimitedLoggableClientRequest;
import io.gravitee.gateway.core.logging.LimitedLoggableClientResponse;
import io.gravitee.gateway.core.logging.LoggableClientRequest;
import io.gravitee.gateway.core.logging.LoggableClientResponse;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableRequestProcessor extends AbstractProcessor<ExecutionContext> {

    private final Logger logger = LoggerFactory.getLogger(LoggableRequestProcessor.class);

    private final ConditionEvaluator evaluator;

    public LoggableRequestProcessor(final ConditionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public void handle(ExecutionContext context) {
        try {
            boolean condition = evaluate(context);

            if (condition) {
                int maxSizeLogMessage = LoggingUtils.getMaxSizeLogMessage(context);

                ((MutableExecutionContext) context).request(
                        maxSizeLogMessage == -1
                            ? new LoggableClientRequest(context.request(), context)
                            : new LimitedLoggableClientRequest(context.request(), context, maxSizeLogMessage)
                    );
                ((MutableExecutionContext) context).response(
                        maxSizeLogMessage == -1
                            ? new LoggableClientResponse(context.request(), context.response(), context)
                            : new LimitedLoggableClientResponse(context.request(), context.response(), context, maxSizeLogMessage)
                    );
            }
        } catch (Exception ex) {
            logger.warn(
                "Unexpected error while evaluating logging condition for the API {} and context path {} : {}",
                context.getAttribute(ExecutionContext.ATTR_API),
                context.getAttribute(ExecutionContext.ATTR_CONTEXT_PATH),
                ex.getMessage()
            );
        }

        next.handle(context);
    }

    protected boolean evaluate(ExecutionContext context) throws Exception {
        return evaluator.evaluate(context, context.request());
    }
}
