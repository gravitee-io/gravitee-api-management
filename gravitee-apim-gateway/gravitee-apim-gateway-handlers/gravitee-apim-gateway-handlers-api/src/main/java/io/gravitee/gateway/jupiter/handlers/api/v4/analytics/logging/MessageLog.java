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
package io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.core.v4.analytics.LoggingContext;
import io.gravitee.reporter.api.v4.common.Message;
import java.util.HashMap;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class MessageLog extends Message {

    protected final LoggingContext loggingContext;

    protected MessageLog(final LoggingContext loggingContext, final io.gravitee.gateway.jupiter.api.message.Message message) {
        this.loggingContext = loggingContext;

        this.setId(message.id());
        this.setError(message.error());

        if (isMessageLogPayload() && message.content() != null) {
            this.setPayload(message.content().toString());
        }

        if (isMessageLogHeaders()) {
            this.setHeaders(HttpHeaders.create(message.headers()));
        }

        if (isMessageLogMetadata()) {
            this.setMetadata(new HashMap<>(message.metadata()));
        }
    }

    protected abstract boolean isMessageLogPayload();

    protected abstract boolean isMessageLogHeaders();

    protected abstract boolean isMessageLogMetadata();
}
