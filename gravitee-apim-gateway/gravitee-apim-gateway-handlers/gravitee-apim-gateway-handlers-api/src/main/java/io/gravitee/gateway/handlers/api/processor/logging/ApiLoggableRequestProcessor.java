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
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.logging.condition.evaluation.el.ExpressionLanguageBasedConditionEvaluator;
import io.gravitee.gateway.core.logging.processor.LoggableRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiLoggableRequestProcessor extends LoggableRequestProcessor {

    private final Logger logger = LoggerFactory.getLogger(ApiLoggableRequestProcessor.class);

    private final LoggingMode mode;
    private int maxSizeLogMessage = -1;
    private String excludedResponseTypes;

    public ApiLoggableRequestProcessor(Logging logging) {
        super(new ExpressionLanguageBasedConditionEvaluator(logging.getCondition()));
        this.mode = logging.getMode();
    }

    @Override
    protected boolean evaluate(ExecutionContext context) throws Exception {
        boolean evaluate = super.evaluate(context);
        if (evaluate) {
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.client", mode.isClientMode());
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.proxy", mode.isProxyMode());
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.max.size.log.message", maxSizeLogMessage);
            context.setAttribute(ExecutionContext.ATTR_PREFIX + "logging.response.excluded.types", excludedResponseTypes);

            return mode.isClientMode();
        }

        return false;
    }

    /**
     * Define the max size of the logging (for each payload, whatever it's about the request / response / consumer / proxy)
     * Which means that if size is define to 5, it will be 5 x 4 = 20 (at most).
     *
     * For backward compatibility, we are considering that default unit is MB
     * @param maxSizeLogMessage
     */
    public void setMaxSizeLogMessage(String maxSizeLogMessage) {
        // log max size limit is in MB format
        // -1 means no limit
        if (maxSizeLogMessage != null) {
            try {
                int value = Integer.parseInt(maxSizeLogMessage);
                if (value >= 0) {
                    // By default, consider MB
                    this.maxSizeLogMessage = Integer.parseInt(maxSizeLogMessage) * (1024 * 1024);
                }
            } catch (NumberFormatException nfe) {
                maxSizeLogMessage = maxSizeLogMessage.toUpperCase();

                try {
                    if (maxSizeLogMessage.endsWith("MB") || maxSizeLogMessage.endsWith("M")) {
                        int value = Integer.parseInt(maxSizeLogMessage.substring(0, maxSizeLogMessage.indexOf('M')));
                        this.maxSizeLogMessage = value * (1024 * 1024);
                    } else if (maxSizeLogMessage.endsWith("KB") || maxSizeLogMessage.endsWith("K")) {
                        int value = Integer.parseInt(maxSizeLogMessage.substring(0, maxSizeLogMessage.indexOf('K')));
                        this.maxSizeLogMessage = value * (1024);
                    } else if (maxSizeLogMessage.endsWith("B")) {
                        this.maxSizeLogMessage = Integer.parseInt(maxSizeLogMessage.substring(0, maxSizeLogMessage.indexOf('B')));
                    } else {
                        logger.error("Max size for API logging is invalid, no limit is defined. (value: {})", maxSizeLogMessage);
                        this.maxSizeLogMessage = -1;
                    }
                } catch (NumberFormatException nfe2) {
                    logger.error("Max size for API logging is invalid, no limit is defined. (value: {})", maxSizeLogMessage);
                    this.maxSizeLogMessage = -1;
                }
            }
        }
    }

    public int getMaxSizeLogMessage() {
        return maxSizeLogMessage;
    }

    public void setExcludedResponseTypes(String excludedResponseTypes) {
        this.excludedResponseTypes = excludedResponseTypes;
    }
}
