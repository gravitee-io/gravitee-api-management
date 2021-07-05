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
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeysHandler extends AbstractHandler {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    public void handle(RoutingContext ctx) {
        final JsonObject searchPayload = ctx.getBodyAsJson();

        // Parse criteria
        final ApiKeyCriteria apiKeyCriteria = readCriteria(searchPayload);

        ctx.vertx().executeBlocking(promise -> {
            try {
                promise.complete(apiKeyRepository.findByCriteria(apiKeyCriteria));
            } catch (TechnicalException te) {
                LOGGER.error("Unable to search for API Keys", te);
                promise.fail(te);
            }
        }, (Handler<AsyncResult<List<ApiKey>>>) result -> handleResponse(ctx, result));
    }

    private ApiKeyCriteria readCriteria(JsonObject payload) {
        ApiKeyCriteria.Builder builder = new ApiKeyCriteria.Builder();

        Long fromVal = payload.getLong("from");
        if (fromVal != null && fromVal > 0) {
            builder.from(fromVal);
        }

        Long toVal = payload.getLong("to");
        if (toVal != null && toVal > 0) {
            builder.to(toVal);
        }

        Boolean revokeVal = payload.getBoolean("includeRevoked", false);
        builder.includeRevoked(revokeVal);

        JsonArray plansArr = payload.getJsonArray("plans");
        if (plansArr != null) {
            Set<String> plans = plansArr.stream().map(obj -> (String) obj).collect(Collectors.toSet());
            builder.plans(plans);
        }

        return builder.build();
    }
}
