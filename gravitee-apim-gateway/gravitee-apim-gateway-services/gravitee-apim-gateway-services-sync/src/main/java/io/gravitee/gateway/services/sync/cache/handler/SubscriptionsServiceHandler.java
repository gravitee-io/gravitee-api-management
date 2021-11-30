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
package io.gravitee.gateway.services.sync.cache.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsServiceHandler implements Handler<RoutingContext> {

    private final Logger LOGGER = LoggerFactory.getLogger(SubscriptionsServiceHandler.class);

    private final ThreadPoolExecutor executorService;

    public SubscriptionsServiceHandler(ThreadPoolExecutor executorService) {
        this.executorService = executorService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response.setStatusCode(HttpStatusCode.OK_200);
        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        response.setChunked(true);

        try {
            final ObjectMapper objectMapper = DatabindCodec.prettyMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            response.write(objectMapper.writeValueAsString(new ExecutorStatistics()));
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        }

        response.end();
    }

    private class ExecutorStatistics {

        public int getCorePoolSize() {
            return SubscriptionsServiceHandler.this.executorService.getCorePoolSize();
        }

        public int getMaximumPoolSize() {
            return SubscriptionsServiceHandler.this.executorService.getMaximumPoolSize();
        }

        public int getLargestPoolSize() {
            return SubscriptionsServiceHandler.this.executorService.getLargestPoolSize();
        }

        public int getPoolSize() {
            return SubscriptionsServiceHandler.this.executorService.getPoolSize();
        }

        public int getActiveCount() {
            return SubscriptionsServiceHandler.this.executorService.getActiveCount();
        }

        public long getTaskCount() {
            return SubscriptionsServiceHandler.this.executorService.getTaskCount();
        }

        public long getCompletedTaskCount() {
            return SubscriptionsServiceHandler.this.executorService.getCompletedTaskCount();
        }
    }
}
