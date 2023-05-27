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
package io.gravitee.repository.bridge.server.handler.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.AsyncResult;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    protected WorkerExecutor bridgeWorkerExecutor;

    protected AbstractHandler(WorkerExecutor bridgeWorkerExecutor) {
        this.bridgeWorkerExecutor = bridgeWorkerExecutor;
    }

    protected <T> void handleResponse(final RoutingContext ctx, AsyncResult<T> result) {
        final HttpServerResponse response = ctx.response();

        try {
            if (result.succeeded()) {
                T data = result.result();
                if (data != null) {
                    final Class<?> dataClass = data.getClass();
                    if (Optional.class.isAssignableFrom(dataClass)) {
                        Optional<T> opt = (Optional<T>) (data);
                        data = opt.orElse(null);
                    }
                }

                if (data != null) {
                    response.setStatusCode(HttpStatusCode.OK_200);
                    response.putHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                    response.setChunked(true);
                    writeData(response, data);
                } else {
                    response.setStatusCode(HttpStatusCode.NOT_FOUND_404);
                    response.end();
                }
            } else {
                endWithError(result.cause(), response);
            }
        } catch (Exception ex) {
            endWithError(result.cause(), response);
        }
    }

    private <T> void writeData(final HttpServerResponse response, final T data) {
        try {
            final ObjectMapper objectMapper = DatabindCodec.prettyMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.writeValue(new OutputWriterStream(response), data);
        } catch (Exception e) {
            LOGGER.error("Unable to transform data object to JSON", e);
            endWithError(e, response);
        }
    }

    private void endWithError(final Throwable throwable, final HttpServerResponse response) {
        LOGGER.error("Unexpected error from the bridge", throwable);
        response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        String errorMessage = throwable.getMessage();
        if (errorMessage == null) {
            errorMessage = "Unexpected error from the bridge";
        }
        response.end(errorMessage);
    }

    protected Set<String> readListParam(String strList) {
        if (StringUtils.hasText(strList)) {
            return Stream.of(strList.split(",")).collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }
}
