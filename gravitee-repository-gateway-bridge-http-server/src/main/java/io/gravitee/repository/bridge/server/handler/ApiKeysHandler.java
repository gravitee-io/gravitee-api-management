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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeysHandler implements Handler<RoutingContext> {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiKeysHandler.class);

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();

        try {
            JsonObject searchPayload = ctx.getBodyAsJson();
            ApiKeyCriteria apiKeyCriteria = readCriteria(searchPayload);
            List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(apiKeyCriteria);

            if (apiKeys == null) {
                response.setStatusCode(HttpStatusCode.NOT_FOUND_404);
            } else {
                response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.setStatusCode(HttpStatusCode.OK_200);
                response.setChunked(true);

                Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                response.write(Json.prettyMapper.writeValueAsString(apiKeys));
            }
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        } catch (TechnicalException te) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to search for API Keys", te);
        }

        response.end();
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
