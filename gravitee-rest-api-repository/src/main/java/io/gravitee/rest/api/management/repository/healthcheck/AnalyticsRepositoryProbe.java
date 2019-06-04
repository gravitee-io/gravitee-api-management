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
package io.gravitee.rest.api.management.repository.healthcheck;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.count.CountQuery;
import io.gravitee.rest.api.management.repository.vertx.VertxCompletableFuture;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsRepositoryProbe implements Probe {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private Vertx vertx;

    @Override
    public String id() {
        return "repository-analytics";
    }

    @Override
    public CompletableFuture<Result> check() {
        Future<Result> future = Future.future();

        vertx.executeBlocking(new Handler<Future<Result>>() {
            @Override
            public void handle(Future<Result> event) {
                try {
                    analyticsRepository.query(new CountQuery());
                    event.complete(Result.healthy());
                } catch (Exception ex) {
                    event.complete(Result.unhealthy(ex));
                }
            }
        }, new Handler<AsyncResult<Result>>() {
            @Override
            public void handle(AsyncResult<Result> event) {
                future.complete(event.result());
            }
        });

        return VertxCompletableFuture.from(vertx, future);
    }
}
