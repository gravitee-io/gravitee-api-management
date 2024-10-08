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
package io.gravitee.gateway.reactive.handlers.api.processor.shutdown;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.node.api.Node;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ShutdownProcessor implements Processor {

    public static final String ID = "processor-shutdown";
    private final Node node;

    public ShutdownProcessor(final Node node) {
        this.node = node;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            if (node.lifecycleState() != Lifecycle.State.STARTED) {
                // The node is certainly shutting down, explicitly ask for closing connection.
                if (ctx.request().version() == HttpVersion.HTTP_2) {
                    // Create a fake internal header to notify the underlying layer to gracefully close the connection (aka: goAway).
                    ctx.response().headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_GO_AWAY);
                } else {
                    ctx.response().headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                }
            }
        });
    }
}
