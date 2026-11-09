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
package io.gravitee.repository.elasticsearch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.platform.repository.api.Scope;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ElasticsearchHttpClientAuthenticationTest {

    private static final String PROPERTY_PREFIX = "analytics.elasticsearch.";
    private static final String NO_AUTHORIZATION = "<none>";
    private static final String ES_INFO =
        "{\"name\":\"node\",\"cluster_name\":\"cluster\",\"version\":{\"number\":\"8.17.2\",\"lucene_version\":\"9.12.0\"}}";

    private final BlockingQueue<String> authorizationHeaders = new LinkedBlockingQueue<>();
    private Vertx vertx;
    private HttpServer elasticsearch;
    private AnnotationConfigApplicationContext context;

    @Configuration
    static class ClientConfiguration {

        @Bean
        public Vertx vertx() {
            return Vertx.vertx();
        }

        @Bean
        public Client client(Environment environment) {
            return new ElasticsearchHttpClientFactory(environment, Scope.ANALYTICS.getName()).createHttpClient();
        }
    }

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        elasticsearch = vertx
            .createHttpServer()
            .requestHandler(request -> {
                authorizationHeaders.add(Objects.requireNonNullElse(request.getHeader("Authorization"), NO_AUTHORIZATION));
                request.response().putHeader("Content-Type", "application/json").end(ES_INFO).subscribe();
            })
            .rxListen(0)
            .blockingGet();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
        vertx.rxClose().blockingAwait();
    }

    @Test
    void should_send_the_api_key_when_only_an_api_key_is_configured() throws Exception {
        startClient(Map.of(PROPERTY_PREFIX + "security.api_key", "encoded-key"));

        assertThat(firstAuthorizationHeader()).isEqualTo("ApiKey encoded-key");
    }

    @Test
    void should_send_basic_credentials_when_only_username_and_password_are_configured() throws Exception {
        startClient(Map.of(PROPERTY_PREFIX + "security.username", "user", PROPERTY_PREFIX + "security.password", "secret"));

        assertThat(firstAuthorizationHeader()).isEqualTo(
            "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void should_send_no_credentials_when_none_are_configured() throws Exception {
        startClient(Map.of());

        assertThat(firstAuthorizationHeader()).isEqualTo(NO_AUTHORIZATION);
    }

    @Test
    void should_fail_to_start_when_both_an_api_key_and_basic_credentials_are_configured() {
        assertThatThrownBy(() ->
            startClient(
                Map.of(
                    PROPERTY_PREFIX + "security.api_key",
                    "encoded-key",
                    PROPERTY_PREFIX + "security.username",
                    "user",
                    PROPERTY_PREFIX + "security.password",
                    "secret"
                )
            )
        )
            .rootCause()
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'analytics.elasticsearch.security.api_key'")
            .hasMessageContaining("'analytics.elasticsearch.security.username'")
            .hasMessageContaining("'analytics.elasticsearch.security.password'");
    }

    private void startClient(Map<String, Object> properties) {
        final Map<String, Object> allProperties = new HashMap<>(properties);
        allProperties.put(PROPERTY_PREFIX + "endpoints[0]", "http://localhost:" + elasticsearch.actualPort());

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", allProperties));
        context.register(ClientConfiguration.class);
        context.refresh();
        context.getBean(Client.class).getInfo().blockingGet();
    }

    private String firstAuthorizationHeader() throws InterruptedException {
        return authorizationHeaders.poll(10, TimeUnit.SECONDS);
    }
}
