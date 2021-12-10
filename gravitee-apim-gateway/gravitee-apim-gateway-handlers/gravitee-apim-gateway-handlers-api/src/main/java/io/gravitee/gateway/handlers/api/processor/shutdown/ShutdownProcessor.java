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
package io.gravitee.gateway.handlers.api.processor.shutdown;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.node.api.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ShutdownProcessor extends AbstractProcessor<ExecutionContext> {

    private final Node node;

    public ShutdownProcessor(final Node node) {
        this.node = node;
    }

    @Override
    public void handle(ExecutionContext context) {
        if(node.lifecycleState() != Lifecycle.State.STARTED) {
            // The node is certainly shutting down, explicitly ask for closing connection.
            if(context.request().version() == HttpVersion.HTTP_2) {
                // Create a fake internal header to notify the underlying layer to gracefully close the connection (aka: goAway).
                context.response().headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_GO_AWAY);
            } else {
                context.response().headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
            }
        }
        next.handle(context);
    }
}