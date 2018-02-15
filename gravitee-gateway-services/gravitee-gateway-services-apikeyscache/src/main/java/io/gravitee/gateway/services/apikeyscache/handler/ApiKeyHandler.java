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
package io.gravitee.gateway.services.apikeyscache.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.services.apikeyscache.ApiKeyRefresher;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyHandler implements Handler<RoutingContext> {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiKeyHandler.class);

    private final Map<String, ApiKeyRefresher> apiKeyRefreshers = new HashMap<>();

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();

        try {
            String sApi = ctx.request().getParam("apiId");
            ApiKeyRefresher apiKeyRefresher = apiKeyRefreshers.get(sApi);

            if (apiKeyRefresher == null) {
                response.setStatusCode(HttpStatusCode.NOT_FOUND_404);
            } else {
                response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.setStatusCode(HttpStatusCode.OK_200);
                response.setChunked(true);

                Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                response.write(Json.prettyMapper.writeValueAsString(new RefresherStatistics(apiKeyRefresher)));
            }
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        }

        response.end();
    }

    public void addRefresher(ApiKeyRefresher apiKeyRefresher) {
        apiKeyRefreshers.put(apiKeyRefresher.getApi().getId(), apiKeyRefresher);
    }

    public void removeRefresher(String api) {
        apiKeyRefreshers.remove(api);
    }

    private class RefresherStatistics {
        private final ApiKeyRefresher refresher;

        RefresherStatistics(ApiKeyRefresher refresher) {
            this.refresher = refresher;
        }

        public long getCount() {
            return refresher.getCount();
        }

        public long getErrorsCount() {
            return refresher.getErrorsCount();
        }

        public long getLastRefreshAt() {
            return refresher.getLastRefreshAt();
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - refresher.getLastRefreshAt();
        }

        public Throwable getLastException() {
            return refresher.getLastException();
        }

        public long getMinTime() {
            return refresher.getMinTime();
        }

        public long getMaxTime() {
            return refresher.getMaxTime();
        }

        public long getAvgTime() {
            return refresher.getAvgTime();
        }

        public long getTotalTime() {
            return refresher.getTotalTime();
        }
    }
}
