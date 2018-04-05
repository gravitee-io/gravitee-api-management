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
package io.gravitee.gateway.report.impl.bus;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventBusReporterWrapper implements Reporter, Handler<Message<Reportable>>{

    private final Reporter reporter;
    private final Vertx vertx;

    public EventBusReporterWrapper(final Vertx vertx, final Reporter reporter) {
        this.vertx = vertx;
        this.reporter = reporter;
    }

    @Override
    public void report(Reportable reportable) {
        // Done by the event bus handler
        // See handle method
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return reporter.lifecycleState();
    }

    @Override
    public Object start() throws Exception {
        Object ret = reporter.start();
        vertx.eventBus().consumer(ReporterVerticle.EVENT_BUS_ADDRESS, this);
        return ret;
    }

    @Override
    public Object stop() throws Exception {
        return reporter.stop();
    }

    @Override
    public void handle(Message<Reportable> reportableMsg) {
        Reportable reportable = reportableMsg.body();
        if (reporter.canHandle(reportable)) {
            reporter.report(reportable);
        }
    }
}
