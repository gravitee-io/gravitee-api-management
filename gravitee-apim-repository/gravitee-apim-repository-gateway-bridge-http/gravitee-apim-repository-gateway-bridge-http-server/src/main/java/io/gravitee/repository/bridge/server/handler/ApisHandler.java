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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.RoutingContext;
import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisHandler extends AbstractHandler {

    @Autowired
    private ApiRepository apiRepository;

    public ApisHandler(WorkerExecutor bridgeWorkerExecutor) {
        super(bridgeWorkerExecutor);
    }

    public void search(RoutingContext ctx) {
        final boolean excludeDefinition = Boolean.parseBoolean(ctx.request().getParam("excludeDefinition"));
        final boolean excludePicture = Boolean.parseBoolean(ctx.request().getParam("excludePicture"));
        final ApiFieldFilter.Builder builder = new ApiFieldFilter.Builder();

        if (excludeDefinition) {
            builder.excludeDefinition();
        }
        if (excludePicture) {
            builder.excludePicture();
        }

        bridgeWorkerExecutor.executeBlocking(
            promise -> {
                try {
                    promise.complete(apiRepository.search(null, builder.build()));
                } catch (Exception te) {
                    LOGGER.error("Unable to search for APIs", te);
                    promise.fail(te);
                }
            },
            false,
            (Handler<AsyncResult<Collection<Api>>>) result -> handleResponse(ctx, result)
        );
    }

    public void findById(RoutingContext ctx) {
        final String sApi = ctx.request().getParam("apiId");

        bridgeWorkerExecutor.executeBlocking(
            promise -> {
                try {
                    promise.complete(apiRepository.findById(sApi));
                } catch (TechnicalException te) {
                    LOGGER.error("Unable to find an API", te);
                    promise.fail(te);
                }
            },
            false,
            (Handler<AsyncResult<Optional<Api>>>) result -> handleResponse(ctx, result)
        );
    }
}
