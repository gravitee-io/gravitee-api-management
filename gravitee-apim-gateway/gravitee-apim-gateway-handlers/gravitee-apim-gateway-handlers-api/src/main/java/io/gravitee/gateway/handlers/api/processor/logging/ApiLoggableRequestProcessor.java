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
package io.gravitee.gateway.handlers.api.processor.logging;

import io.gravitee.definition.model.Logging;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.core.logging.condition.evaluation.el.ExpressionLanguageBasedConditionEvaluator;
import io.gravitee.gateway.core.logging.processor.LoggableRequestProcessor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiLoggableRequestProcessor extends LoggableRequestProcessor {

    private final LoggingContext loggingContext;

    public ApiLoggableRequestProcessor(Logging logging) {
        super(new ExpressionLanguageBasedConditionEvaluator(logging.getCondition()));
        this.loggingContext = new LoggingContext(logging);
    }

    @Override
    protected boolean evaluate(ExecutionContext context) throws Exception {
        boolean evaluate = super.evaluate(context);
        if (evaluate) {
            context.setAttribute(LoggingContext.LOGGING_ATTRIBUTE, loggingContext);
            return loggingContext.clientMode();
        }

        return false;
    }

    public void setMaxSizeLogMessage(String maxSizeLogMessage) {
        this.loggingContext.setMaxSizeLogMessage(maxSizeLogMessage);
    }

    public int getMaxSizeLogMessage() {
        return this.loggingContext.getMaxSizeLogMessage();
    }

    public void setExcludedResponseTypes(String excludedResponseTypes) {
        this.loggingContext.setExcludedResponseTypes(excludedResponseTypes);
    }
}
