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

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import java.util.Collection;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentsHandler extends AbstractHandler {

    @Autowired
    private EnvironmentRepository environmentRepository;

    public void findByOrganizationsAndHrids(RoutingContext ctx) {
        final String organizationsIdsParam = ctx.request().getParam("organizationsIds");
        final String hridsParam = ctx.request().getParam("hrids");

        final Set<String> organizationsIds = readListParam(organizationsIdsParam);
        final Set<String> hrids = readListParam(hridsParam);

        ctx
            .vertx()
            .executeBlocking(
                (Handler<Promise<Set<Environment>>>) promise -> {
                    try {
                        promise.complete(environmentRepository.findByOrganizationsAndHrids(organizationsIds, hrids));
                    } catch (Exception ex) {
                        LOGGER.error("Unable to search for environments", ex);
                        promise.fail(ex);
                    }
                },
                event -> handleResponse(ctx, event)
            );
    }

    public void findAll(RoutingContext ctx) {
        ctx
            .vertx()
            .executeBlocking(
                (Handler<Promise<Collection<Environment>>>) promise -> {
                    try {
                        promise.complete(environmentRepository.findAll());
                    } catch (Exception ex) {
                        LOGGER.error("Unable to search for environments", ex);
                        promise.fail(ex);
                    }
                },
                event -> handleResponse(ctx, event)
            );
    }
}
