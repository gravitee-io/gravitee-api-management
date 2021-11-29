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

import io.gravitee.definition.model.Logging;
import io.gravitee.gateway.api.ExecutionContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggingContext {

    public static final String LOGGING_ATTRIBUTE = ExecutionContext.ATTR_PREFIX + "logging";

    private final Logging logging;

    private int maxSizeLogMessage;
    private String excludedResponseTypes;

    public LoggingContext(final Logging logging) {
        this.logging = logging;
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

    public void setMaxSizeLogMessage(int maxSizeLogMessage) {
        // log max size limit is in MB format
        // -1 means no limit
        this.maxSizeLogMessage = (maxSizeLogMessage <= -1) ? -1 : maxSizeLogMessage * (1024 * 1024);
    }

    public String getExcludedResponseTypes() {
        return excludedResponseTypes;
    }

    public void setExcludedResponseTypes(String excludedResponseTypes) {
        this.excludedResponseTypes = excludedResponseTypes;
    }
}
