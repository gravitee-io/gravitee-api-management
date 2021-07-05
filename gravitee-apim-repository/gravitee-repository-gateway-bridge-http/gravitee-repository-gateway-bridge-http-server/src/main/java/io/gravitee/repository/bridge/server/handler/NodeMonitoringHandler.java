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
package io.gravitee.repository.bridge.server.handler;

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeMonitoringHandler extends AbstractHandler {

    @Autowired
    private NodeMonitoringRepository nodeMonitoringRepository;

    public void create(RoutingContext ctx) {
        ctx.vertx().executeBlocking(promise -> {
            try {
                Monitoring monitoring = ctx.getBodyAsJson().mapTo(Monitoring.class);
                nodeMonitoringRepository.create(monitoring)
                        .subscribe(
                                promise::complete,
                                throwable -> {
                                    LOGGER.error("Unable to create a node monitoring", throwable);
                                    promise.fail(throwable);
                                });
            } catch (Exception ex) {
                LOGGER.error("Unable to create a node monitoring", ex);
                promise.fail(ex);
            }
        }, (Handler<AsyncResult<Monitoring>>) event -> handleResponse(ctx, event));
    }

    public void update(RoutingContext ctx) {
        ctx.vertx().executeBlocking(promise -> {
            try {
                Monitoring monitoring = ctx.getBodyAsJson().mapTo(Monitoring.class);
                nodeMonitoringRepository.update(monitoring)
                        .subscribe(
                                promise::complete,
                                throwable -> {
                                    LOGGER.error("Unable to update a node monitoring", throwable);
                                    promise.fail(throwable);
                                });
            } catch (Exception ex) {
                LOGGER.error("Unable to update a node monitoring", ex);
                promise.fail(ex);
            }
        }, (Handler<AsyncResult<Monitoring>>) event -> handleResponse(ctx, event));
    }

    public void findByNodeIdAndType(RoutingContext ctx) {
        ctx.vertx().executeBlocking(promise -> {
            try {
                nodeMonitoringRepository.findByNodeIdAndType(
                        ctx.request().getParam("nodeId"),
                        ctx.request().getParam("type"))
                        .subscribe(
                                monitoring -> promise.complete(Optional.of(monitoring)),
                                throwable -> {
                                    LOGGER.error("Unable to find a node monitoring by type and ID", throwable);
                                    promise.fail(throwable);
                                }, () -> promise.complete(Optional.empty()));
            } catch (Exception ex) {
                LOGGER.error("Unable to find a node monitoring by type and ID", ex);
                promise.fail(ex);
            }
        }, (Handler<AsyncResult<Optional<Monitoring>>>) event -> handleResponse(ctx, event));
    }
}
