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

import static io.gravitee.repository.bridge.server.utils.ParamUtils.readPageable;
import static io.gravitee.repository.bridge.server.utils.ParamUtils.readSortable;
import static java.util.Collections.emptyList;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.WorkerExecutor;
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

    public SubscriptionsHandler(WorkerExecutor bridgeWorkerExecutor) {
        super(bridgeWorkerExecutor);
    }

    public void search(RoutingContext ctx) {
        final JsonObject searchPayload = ctx.body().asJsonObject();
        final SubscriptionCriteria subscriptionCriteria = readCriteria(searchPayload);
        final Sortable sortable = readSortable(ctx);

        bridgeWorkerExecutor.executeBlocking(
            promise -> {
                try {
                    promise.complete(subscriptionRepository.search(subscriptionCriteria, sortable));
                } catch (TechnicalException te) {
                    LOGGER.error("Unable to search for subscriptions", te);
                    promise.fail(te);
                }
            },
            false,
            (Handler<AsyncResult<List<Subscription>>>) result -> handleResponse(ctx, result)
        );
    }

    public void searchPageable(RoutingContext ctx) {
        final JsonObject searchPayload = ctx.body().asJsonObject();
        final SubscriptionCriteria subscriptionCriteria = readCriteria(searchPayload);
        final Sortable sortable = readSortable(ctx);
        final Pageable pageable = readPageable(ctx);

        bridgeWorkerExecutor.executeBlocking(
            promise -> {
                try {
                    promise.complete(subscriptionRepository.search(subscriptionCriteria, sortable, pageable));
                } catch (TechnicalException te) {
                    LOGGER.error("Unable to search for subscriptions", te);
                    promise.fail(te);
                }
            },
            false,
            (Handler<AsyncResult<Page<Subscription>>>) result -> handleResponse(ctx, result)
        );
    }

    public void findByIds(RoutingContext ctx) {
        List<String> ids = ctx.body().asJsonArray() == null
            ? emptyList()
            : ctx.body().asJsonArray().stream().map(String::valueOf).collect(Collectors.toList());

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
        SubscriptionCriteria.SubscriptionCriteriaBuilder builder = SubscriptionCriteria.builder();
        if (payload != null) {
            Long fromVal = payload.getLong("from");
            if (fromVal != null && fromVal > 0) {
                builder.from(fromVal);
            }

            Long toVal = payload.getLong("to");
            if (toVal != null && toVal > 0) {
                builder.to(toVal);
            }

            String clientIdVal = payload.getString("clientId");
            if (clientIdVal != null && !clientIdVal.isEmpty()) {
                builder.clientId(clientIdVal);
            }

            JsonArray idsArr = payload.getJsonArray("ids");
            if (idsArr != null) {
                Set<String> ids = idsArr.stream().map(String.class::cast).collect(Collectors.toSet());
                builder.ids(ids);
            }

            JsonArray apisArr = payload.getJsonArray("apis");
            if (apisArr != null) {
                Set<String> apis = apisArr.stream().map(String.class::cast).collect(Collectors.toSet());
                builder.apis(apis);
            }

            JsonArray plansArr = payload.getJsonArray("plans");
            if (plansArr != null) {
                Set<String> plans = plansArr.stream().map(String.class::cast).collect(Collectors.toSet());
                builder.plans(plans);
            }

            JsonArray applicationsArr = payload.getJsonArray("applications");
            if (applicationsArr != null) {
                Set<String> applications = applicationsArr.stream().map(String.class::cast).collect(Collectors.toSet());
                builder.applications(applications);
            }

            JsonArray statusArr = payload.getJsonArray("statuses");
            if (statusArr != null) {
                Set<String> statuses = statusArr.stream().map(String::valueOf).collect(Collectors.toSet());
                builder.statuses(statuses);
            }

            JsonArray planSecurityTypesArr = payload.getJsonArray("planSecurityTypes");
            if (planSecurityTypesArr != null) {
                Set<String> planSecurityTypes = planSecurityTypesArr.stream().map(String::valueOf).collect(Collectors.toSet());
                builder.planSecurityTypes(planSecurityTypes);
            }

            Long endingAtAfterVal = payload.getLong("endingAtAfter");
            if (endingAtAfterVal != null && endingAtAfterVal > 0) {
                builder.endingAtAfter(endingAtAfterVal);
            }

            Long endingAtBeforeVal = payload.getLong("endingAtBefore");
            if (endingAtBeforeVal != null && endingAtBeforeVal > 0) {
                builder.endingAtBefore(endingAtBeforeVal);
            }

            Boolean includeWithoutEndVal = payload.getBoolean("includeWithoutEnd");
            if (includeWithoutEndVal != null) {
                builder.includeWithoutEnd(includeWithoutEndVal);
            }
        }

        return builder.build();
    }
}
