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
package io.gravitee.gateway.jupiter.core.v4.analytics;

import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.definition.model.MessageConditionSupplier;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingContext implements ConditionSupplier, MessageConditionSupplier {

    private static final String DEFAULT_EXCLUDED_CONTENT_TYPES =
        "video.*|audio.*|image.*|application\\/octet-stream|application\\/pdf|text\\/event-stream";

    private final Logging logging;
    private int maxSizeLogMessage = -1;
    private String excludedResponseTypes;
    private Pattern excludedContentTypesPattern;

    @Override
    public String getCondition() {
        return logging.getCondition();
    }

    @Override
    public String getMessageCondition() {
        return logging.getMessageCondition();
    }

    public boolean entrypointRequest() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isRequest();
    }

    public boolean entrypointResponse() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isResponse();
    }

    public boolean endpointRequest() {
        return logging.getMode().isEndpoint() && logging.getPhase().isRequest();
    }

    public boolean endpointResponse() {
        return logging.getMode().isEndpoint() && logging.getPhase().isResponse();
    }

    public boolean entrypointRequestHeaders() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isRequest() && logging.getContent().isHeaders();
    }

    public boolean entrypointRequestPayload() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isRequest() && logging.getContent().isPayload();
    }

    public boolean entrypointRequestMessageHeaders() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isRequest() && logging.getContent().isMessageHeaders();
    }

    public boolean entrypointRequestMessagePayload() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isRequest() && logging.getContent().isMessagePayload();
    }

    public boolean entrypointRequestMessageMetadata() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isRequest() && logging.getContent().isMessageMetadata();
    }

    public boolean endpointRequestHeaders() {
        return logging.getMode().isEndpoint() && logging.getPhase().isRequest() && logging.getContent().isHeaders();
    }

    public boolean endpointRequestPayload() {
        return logging.getMode().isEndpoint() && logging.getPhase().isRequest() && logging.getContent().isPayload();
    }

    public boolean endpointRequestMessageHeaders() {
        return logging.getMode().isEndpoint() && logging.getPhase().isRequest() && logging.getContent().isMessageHeaders();
    }

    public boolean endpointRequestMessagePayload() {
        return logging.getMode().isEndpoint() && logging.getPhase().isRequest() && logging.getContent().isMessagePayload();
    }

    public boolean endpointRequestMessageMetadata() {
        return logging.getMode().isEndpoint() && logging.getPhase().isRequest() && logging.getContent().isMessageMetadata();
    }

    public boolean entrypointResponseHeaders() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isResponse() && logging.getContent().isHeaders();
    }

    public boolean entrypointResponsePayload() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isResponse() && logging.getContent().isPayload();
    }

    public boolean entrypointResponseMessageHeaders() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isResponse() && logging.getContent().isMessageHeaders();
    }

    public boolean entrypointResponseMessagePayload() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isResponse() && logging.getContent().isMessagePayload();
    }

    public boolean entrypointResponseMessageMetadata() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isResponse() && logging.getContent().isMessageMetadata();
    }

    public boolean endpointResponseHeaders() {
        return logging.getMode().isEndpoint() && logging.getPhase().isResponse() && logging.getContent().isHeaders();
    }

    public boolean endpointResponsePayload() {
        return logging.getMode().isEndpoint() && logging.getPhase().isResponse() && logging.getContent().isPayload();
    }

    public boolean endpointResponseMessageHeaders() {
        return logging.getMode().isEndpoint() && logging.getPhase().isResponse() && logging.getContent().isMessageHeaders();
    }

    public boolean endpointResponseMessagePayload() {
        return logging.getMode().isEndpoint() && logging.getPhase().isResponse() && logging.getContent().isMessagePayload();
    }

    public boolean endpointResponseMessageMetadata() {
        return logging.getMode().isEndpoint() && logging.getPhase().isResponse() && logging.getContent().isMessageMetadata();
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
                        log.error("Max size for API logging is invalid, no limit is defined. (value: {})", maxSizeLogMessage);
                        this.maxSizeLogMessage = -1;
                    }
                } catch (NumberFormatException nfe2) {
                    log.error("Max size for API logging is invalid, no limit is defined. (value: {})", maxSizeLogMessage);
                    this.maxSizeLogMessage = -1;
                }
            }
        }
    }

    public String getExcludedResponseTypes() {
        return excludedResponseTypes;
    }

    public void setExcludedResponseTypes(final String excludedResponseTypes) {
        this.excludedResponseTypes = excludedResponseTypes;
    }

    public boolean isContentTypeLoggable(final String contentType) {
        // init pattern
        if (excludedContentTypesPattern == null) {
            try {
                excludedContentTypesPattern = Pattern.compile(excludedResponseTypes);
            } catch (Exception e) {
                excludedContentTypesPattern = Pattern.compile(DEFAULT_EXCLUDED_CONTENT_TYPES);
            }
        }

        return contentType == null || !excludedContentTypesPattern.matcher(contentType).find();
    }
}
