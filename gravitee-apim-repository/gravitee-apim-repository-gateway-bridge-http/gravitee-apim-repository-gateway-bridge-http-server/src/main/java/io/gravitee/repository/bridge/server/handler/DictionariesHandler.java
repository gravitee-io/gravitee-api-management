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
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DictionariesHandler extends AbstractHandler {

    @Autowired
    private DictionaryRepository dictionaryRepository;

    public void find(RoutingContext ctx) {
        final String environementsIdsParam = ctx.request().getParam("environmentsIds");
        if (StringUtils.isEmpty(environementsIdsParam)) {
            findAll(ctx);
        } else {
            findAllByEnvironment(ctx, environementsIdsParam);
        }
    }

    private void findAllByEnvironment(RoutingContext ctx, String environementsIdsParam) {
        final Set<String> environments = readListParam(environementsIdsParam);

        ctx
            .vertx()
            .executeBlocking(
                (Handler<Promise<Set<Dictionary>>>) promise -> {
                    try {
                        promise.complete(dictionaryRepository.findAllByEnvironments(environments));
                    } catch (Exception ex) {
                        LOGGER.error("Unable to search for dictionaries", ex);
                        promise.fail(ex);
                    }
                },
                event -> handleResponse(ctx, event)
            );
    }

    private void findAll(RoutingContext ctx) {
        ctx
            .vertx()
            .executeBlocking(
                promise -> {
                    try {
                        promise.complete(dictionaryRepository.findAll());
                    } catch (TechnicalException te) {
                        LOGGER.error("Unable to get dictionaries", te);
                        promise.fail(te);
                    }
                },
                (Handler<AsyncResult<Set<Dictionary>>>) result -> handleResponse(ctx, result)
            );
    }
}
