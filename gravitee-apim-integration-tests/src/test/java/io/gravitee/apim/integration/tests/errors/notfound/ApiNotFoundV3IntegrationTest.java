/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.errors.notfound;

import static org.assertj.core.api.Assertions.*;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ExecutionMode;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiNotFoundV3IntegrationTest {

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    public class NoErrorMessageOverride extends AbstractGatewayTest {

        @Test
        void should_not_found_api(HttpClient client) {
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(404);
                    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN);
                    return response.body();
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response).hasToString("No context-path matches the request URI.");
                    return true;
                });
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    class ErrorMessageOverride extends AbstractGatewayTest {

        private JsonObject errorMessage;

        @Override
        protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
            errorMessage = new JsonObject();
            errorMessage.put("error", "This is the new not found message");

            gatewayConfigurationBuilder
                .set("http.errors[404].message", errorMessage)
                .set("http.errors[404].contentType", MediaType.APPLICATION_JSON);
        }

        @Test
        void should_not_found_api(HttpClient client) {
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(404);
                    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
                    assertThat(response.getHeader(HttpHeaders.CONTENT_LENGTH)).isEqualTo(
                        Integer.toString(errorMessage.toBuffer().length())
                    );
                    return response.body();
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response).hasToString(errorMessage.toString());
                    return true;
                });
        }
    }
}
