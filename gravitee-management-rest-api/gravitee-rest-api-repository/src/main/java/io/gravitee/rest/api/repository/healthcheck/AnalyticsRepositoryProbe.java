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
package io.gravitee.rest.api.repository.healthcheck;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.count.CountQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.concurrent.CompletionStage;
import org.springframework.beans.factory.annotation.Autowired;

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
    public CompletionStage<Result> check() {
        Promise<Result> promise = Promise.promise();

        vertx.executeBlocking(
            new Handler<Promise<Result>>() {
                @Override
                public void handle(Promise<Result> event) {
                    try {
                        analyticsRepository.query(new CountQuery());
                        promise.complete(Result.healthy());
                    } catch (Exception ex) {
                        promise.complete(Result.unhealthy(ex));
                    }
                }
            },
            new Handler<AsyncResult<Result>>() {
                @Override
                public void handle(AsyncResult<Result> event) {
                    promise.complete(event.result());
                }
            }
        );

        return promise.future().toCompletionStage();
    }
}
