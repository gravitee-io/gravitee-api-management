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
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(ApisHandler.class);

    @Autowired
    private ApiRepository apiRepository;

    public void findAll(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response.setStatusCode(HttpStatusCode.OK_200);
        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        response.setChunked(true);

        try {
            Collection<Api> apis = apiRepository.findAll();

            Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            response.write(Json.prettyMapper.writeValueAsString(apis));
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        } catch (TechnicalException te) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to search for events", te);
        }

        response.end();
    }

    public void findById(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();

        try {
            String sApi = ctx.request().getParam("apiId");
            Optional<Api> optApi = apiRepository.findById(sApi);

            if (optApi == null || !optApi.isPresent()) {
                response.setStatusCode(HttpStatusCode.NOT_FOUND_404);
            } else {
                response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.setStatusCode(HttpStatusCode.OK_200);
                response.setChunked(true);

                Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                response.write(Json.prettyMapper.writeValueAsString(optApi.get()));
            }
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        } catch (TechnicalException te) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to get an API", te);
        }

        response.end();
    }
}
