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
package io.gravitee.gateway.core.logging;

import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.definition.model.Logging;
import io.gravitee.gateway.api.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggingContext implements ConditionSupplier {

    private final Logger logger = LoggerFactory.getLogger(LoggingContext.class);

    public static final String LOGGING_ATTRIBUTE = ExecutionContext.ATTR_PREFIX + "logging";

    public static final String LOGGING_CONTEXT_ATTRIBUTE =
        io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_INTERNAL_PREFIX + "logging.context";

    private final Logging logging;
    private int maxSizeLogMessage = -1;
    private String excludedResponseTypes;

    public LoggingContext(final Logging logging) {
        this.logging = logging;
    }

    @Override
    public String getCondition() {
        return logging.getCondition();
    }

    public boolean clientMode() {
        return logging.getMode().isClientMode();
    }

    public boolean proxyMode() {
        return logging.getMode().isProxyMode();
    }

    public boolean requestHeaders() {
        return logging.getScope().isRequest() && logging.getContent().isHeaders();
    }

    public boolean requestPayload() {
        return logging.getScope().isRequest() && logging.getContent().isPayloads();
    }

    public boolean responseHeaders() {
        return logging.getScope().isResponse() && logging.getContent().isHeaders();
    }

    public boolean responsePayload() {
        return logging.getScope().isResponse() && logging.getContent().isPayloads();
    }

    public boolean proxyRequestHeaders() {
        return logging.getMode().isProxyMode() && logging.getScope().isRequest() && logging.getContent().isHeaders();
    }

    public boolean proxyRequestPayload() {
        return logging.getMode().isProxyMode() && logging.getScope().isRequest() && logging.getContent().isPayloads();
    }

    public boolean proxyResponseHeaders() {
        return logging.getMode().isProxyMode() && logging.getScope().isResponse() && logging.getContent().isHeaders();
    }

    public boolean proxyResponsePayload() {
        return logging.getMode().isProxyMode() && logging.getScope().isResponse() && logging.getContent().isPayloads();
    }

    public int getMaxSizeLogMessage() {
        return maxSizeLogMessage;
    }

    /**
     * Define the max size of the logging (for each payload, whatever it's about the request / response / consumer / proxy)
     * Which means that if size is define to 5, it will be 5 x 4 = 20 (at most).
     *
     * For backward compatibility, we are considering that default unit is MB
     * @param maxSizeLogMessage Max size, can be a simple number (considered as MB) or a string containing value and unit like `20KB`
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

    public String getExcludedResponseTypes() {
        return excludedResponseTypes;
    }

    public void setExcludedResponseTypes(String excludedResponseTypes) {
        this.excludedResponseTypes = excludedResponseTypes;
    }
}
