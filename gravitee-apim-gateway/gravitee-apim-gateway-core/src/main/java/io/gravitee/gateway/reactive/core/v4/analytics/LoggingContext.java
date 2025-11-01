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
package io.gravitee.gateway.reactive.core.v4.analytics;

import io.gravitee.common.utils.SizeUtils;
import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.gateway.report.guard.LogGuardService;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingContext implements ConditionSupplier {

    private static final String DEFAULT_EXCLUDED_CONTENT_TYPES =
        "video.*|audio.*|image.*|application/octet-stream|application/pdf|text/event-stream";

    protected final Logging logging;

    @Getter
    private int maxSizeLogMessage = -1;

    @Setter
    @Getter
    private String excludedResponseTypes;

    private Pattern excludedContentTypesPattern;

    @Setter
    private LogGuardService logGuardService;

    @Override
    public String getCondition() {
        return logging.getCondition();
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

    public boolean endpointRequestHeaders() {
        return logging.getMode().isEndpoint() && logging.getPhase().isRequest() && logging.getContent().isHeaders();
    }

    public boolean endpointRequestPayload() {
        return logging.getMode().isEndpoint() && logging.getPhase().isRequest() && logging.getContent().isPayload();
    }

    public boolean entrypointResponseHeaders() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isResponse() && logging.getContent().isHeaders();
    }

    public boolean entrypointResponsePayload() {
        return logging.getMode().isEntrypoint() && logging.getPhase().isResponse() && logging.getContent().isPayload();
    }

    public boolean endpointResponseHeaders() {
        return logging.getMode().isEndpoint() && logging.getPhase().isResponse() && logging.getContent().isHeaders();
    }

    public boolean endpointResponsePayload() {
        return logging.getMode().isEndpoint() && logging.getPhase().isResponse() && logging.getContent().isPayload();
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
        try {
            final long sizeInBytes = SizeUtils.toBytes(maxSizeLogMessage);
            this.maxSizeLogMessage = Math.toIntExact(sizeInBytes);
        } catch (ArithmeticException | NumberFormatException ae) {
            this.maxSizeLogMessage = -1;
        }
    }

    public boolean isContentTypeLoggable(final String contentType) {
        // init pattern
        if (excludedContentTypesPattern == null) {
            this.excludedContentTypesPattern = Stream.of(
                logging.getOverrideContentTypeValidation(),
                excludedResponseTypes,
                DEFAULT_EXCLUDED_CONTENT_TYPES
            )
                .filter(pattern -> pattern != null && !pattern.isEmpty())
                .flatMap(pattern -> {
                    try {
                        return Stream.of(Pattern.compile(pattern));
                    } catch (PatternSyntaxException e) {
                        return Stream.empty();
                    }
                })
                .findFirst()
                .orElse(null);
        }

        return contentType == null || !excludedContentTypesPattern.matcher(contentType).find();
    }

    /**
     * Determines if body can be logged by asking the logGuardService
     * if the guard is activated (if logGuardService not available, always
     * return true)
     * @return true if the body can be logged
     */
    public boolean isBodyLoggable() {
        return logGuardService == null || !logGuardService.isLogGuardActive();
    }
}
