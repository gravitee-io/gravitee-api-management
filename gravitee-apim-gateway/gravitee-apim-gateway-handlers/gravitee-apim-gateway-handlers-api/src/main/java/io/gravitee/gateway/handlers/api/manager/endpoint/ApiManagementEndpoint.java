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
package io.gravitee.gateway.handlers.api.manager.endpoint;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.handlers.api.definition.ReactableApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiManagementEndpoint implements Handler<RoutingContext>, ManagementEndpoint {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiManagementEndpoint.class);

    @Autowired
    private ApiManager apiManager;

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/apis/:apiId";
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();

        try {
            String sApi = ctx.request().getParam("apiId");
            ReactableApi api = apiManager.get(sApi);

            if (api == null) {
                response.setStatusCode(HttpStatusCode.NOT_FOUND_404);
            } else {
                response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.setStatusCode(HttpStatusCode.OK_200);
                response.setChunked(true);

                final ObjectMapper objectMapper = DatabindCodec.prettyMapper();
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                response.write(objectMapper.writeValueAsString(api));
            }
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        }

        response.end();
    }
}
