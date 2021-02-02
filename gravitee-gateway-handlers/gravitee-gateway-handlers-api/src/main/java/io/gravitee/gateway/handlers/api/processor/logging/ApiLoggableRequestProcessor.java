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
import io.gravitee.definition.model.LoggingContent;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.LoggingScope;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.logging.condition.evaluation.el.ExpressionLanguageBasedConditionEvaluator;
import io.gravitee.gateway.core.logging.processor.LoggableRequestProcessor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiLoggableRequestProcessor extends LoggableRequestProcessor {

    private final LoggingMode mode;
    private final LoggingContent content;
    private final LoggingScope scope;
    private int maxSizeLogMessage;
    private String excludedResponseTypes;

    public ApiLoggableRequestProcessor(Logging logging) {
        super(new ExpressionLanguageBasedConditionEvaluator(logging.getCondition()));
        this.mode = logging.getMode();
        this.content = logging.getContent();
        this.scope = logging.getScope();
    }

    @Override
    protected boolean evaluate(ExecutionContext context) throws Exception {
        boolean evaluate = super.evaluate(context);
        if (evaluate) {
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.client", mode.isClientMode());
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.proxy", mode.isProxyMode());

            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.max.size.log.message", maxSizeLogMessage);
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.response.excluded.types", excludedResponseTypes);

            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.request.headers", scope.isRequest() && content.isHeaders());
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.request.payloads", scope.isRequest() && content.isPayloads());

            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.response.headers", scope.isResponse() && content.isHeaders());
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.response.payloads", scope.isResponse() && content.isPayloads());

            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.proxy.request.headers", mode.isProxyMode() && scope.isRequest() && content.isHeaders());
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.proxy.request.payloads", mode.isProxyMode() && scope.isRequest() && content.isPayloads());

            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.proxy.response.headers", mode.isProxyMode() && scope.isResponse() && content.isHeaders());
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.proxy.response.payloads", mode.isProxyMode() && scope.isResponse() && content.isPayloads());

            return mode.isClientMode();
        }

        return false;
    }

    public void setMaxSizeLogMessage(int maxSizeLogMessage) {
        // log max size limit is in MB format
        // -1 means no limit
        this.maxSizeLogMessage = (maxSizeLogMessage <= -1) ? -1 : maxSizeLogMessage * (1024 * 1024);
    }

    public void setExcludedResponseTypes(String excludedResponseTypes) {
        this.excludedResponseTypes = excludedResponseTypes;
    }
}
