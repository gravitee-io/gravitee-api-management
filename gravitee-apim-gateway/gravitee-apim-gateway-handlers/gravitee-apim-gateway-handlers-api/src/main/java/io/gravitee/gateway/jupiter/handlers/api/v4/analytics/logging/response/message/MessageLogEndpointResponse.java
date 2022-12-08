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
package io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.response.message;

import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.MessageLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageLogEndpointResponse extends MessageLog {

    public MessageLogEndpointResponse(LoggingContext loggingContext, Message message) {
        super(loggingContext, message);
    }

    @Override
    protected boolean isMessageLogPayload() {
        return loggingContext.endpointResponseMessagePayload();
    }

    @Override
    protected boolean isMessageLogHeaders() {
        return loggingContext.endpointResponseMessageHeaders();
    }

    @Override
    protected boolean isMessageLogMetadata() {
        return loggingContext.endpointResponseMessageMetadata();
    }
}
