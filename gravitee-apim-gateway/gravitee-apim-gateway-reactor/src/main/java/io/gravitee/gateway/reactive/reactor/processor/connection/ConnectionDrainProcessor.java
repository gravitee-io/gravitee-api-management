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
package io.gravitee.gateway.reactive.reactor.processor.connection;

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.core.connection.ConnectionDrainManager;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ConnectionDrainProcessor implements Processor {

    private SimpleDateFormat sdf;

    public static final String ID = "processor-connection-drain";
    private final ConnectionDrainManager connectionDrainManager;

    public ConnectionDrainProcessor(ConnectionDrainManager connectionDrainManager) {
        this.connectionDrainManager = connectionDrainManager;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        if (ctx.request().connectionTimestamp() <= connectionDrainManager.drainRequestedAt()) {
            return Completable.fromRunnable(() -> {
                if (log.isDebugEnabled()) {
                    if (sdf == null) {
                        sdf = new SimpleDateFormat("HH:mm:ss.SSS");
                    }
                    log.debug(
                        "Drain connection started at {} ({})",
                        sdf.format(new Date(ctx.request().connectionTimestamp())),
                        ctx.request().version().name()
                    );
                }
                if (ctx.request().version() == HttpVersion.HTTP_2) {
                    ctx.response().headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_GO_AWAY);
                } else {
                    ctx.response().headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                }
            });
        }

        // No flush requested or connection created way after the flush has been requested.
        return Completable.complete();
    }
}
