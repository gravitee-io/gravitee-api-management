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

import static java.util.Collections.emptyList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsHandler extends AbstractHandler {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public void search(RoutingContext ctx) {
        final JsonObject searchPayload = ctx.getBodyAsJson();
        final SubscriptionCriteria subscriptionCriteria = readCriteria(searchPayload);

        ctx
            .vertx()
            .executeBlocking(
                promise -> {
                    try {
                        promise.complete(subscriptionRepository.search(subscriptionCriteria));
                    } catch (TechnicalException te) {
                        LOGGER.error("Unable to search for subscriptions", te);
                        promise.fail(te);
                    }
                },
                (Handler<AsyncResult<List<Subscription>>>) result -> handleResponse(ctx, result)
            );
    }

    public void findByIds(RoutingContext ctx) {
        List<String> ids = ctx.getBodyAsJsonArray() == null
            ? emptyList()
            : ctx.getBodyAsJsonArray().stream().map(String::valueOf).collect(Collectors.toList());

        ctx
            .vertx()
            .executeBlocking(
                promise -> {
                    if (ids.isEmpty()) {
                        promise.fail("Unable to search for subscriptions by id : ids list is mandatory");
                    }
                    try {
                        promise.complete(subscriptionRepository.findByIdIn(ids));
                    } catch (TechnicalException te) {
                        LOGGER.error("Unable to search for subscriptions by id", te);
                        promise.fail(te);
                    }
                },
                (Handler<AsyncResult<List<Subscription>>>) result -> handleResponse(ctx, result)
            );
    }

    private SubscriptionCriteria readCriteria(JsonObject payload) {
        SubscriptionCriteria.Builder builder = new SubscriptionCriteria.Builder();

        Long fromVal = payload.getLong("from");
        if (fromVal != null && fromVal > 0) {
            builder.from(fromVal);
        }

        Long toVal = payload.getLong("to");
        if (toVal != null && toVal > 0) {
            builder.to(toVal);
        }

        String clientIdVal = payload.getString("clientId");
        if (clientIdVal != null && clientIdVal.isEmpty()) {
            builder.clientId(clientIdVal);
        }

        JsonArray plansArr = payload.getJsonArray("plans");
        if (plansArr != null) {
            Set<String> plans = plansArr.stream().map(obj -> (String) obj).collect(Collectors.toSet());
            builder.plans(plans);
        }

        JsonArray applicationsArr = payload.getJsonArray("applications");
        if (applicationsArr != null) {
            Set<String> applications = applicationsArr.stream().map(obj -> (String) obj).collect(Collectors.toSet());
            builder.applications(applications);
        }

        JsonArray apisArr = payload.getJsonArray("apis");
        if (apisArr != null) {
            Set<String> apis = apisArr.stream().map(obj -> (String) obj).collect(Collectors.toSet());
            builder.apis(apis);
        }

        JsonArray statusArr = payload.getJsonArray("status");
        if (statusArr != null) {
            Set<Subscription.Status> statuses = statusArr
                .stream()
                .map(obj -> Subscription.Status.valueOf((String) obj))
                .collect(Collectors.toSet());
            builder.statuses(statuses);
        }

        return builder.build();
    }
}
