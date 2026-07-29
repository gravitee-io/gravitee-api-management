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
package io.gravitee.apim.integration.tests.messages.httpget;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProvider;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProviderFactory;
import com.graviteesource.secretprovider.hcvault.config.manager.VaultConfig;
import com.graviteesource.service.secrets.SecretsService;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.apim.integration.tests.messages.AbstractKafkaEndpointIntegrationTest;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.assignattributes.AssignAttributesPolicy;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.containers.Container;
import org.testcontainers.vault.VaultContainer;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpGetEntrypointKafkaEndpointIntegrationTest extends AbstractKafkaEndpointIntegrationTest {

    private static final String VAULT_TOKEN = UUID.randomUUID().toString();

    @org.testcontainers.junit.jupiter.Container
    protected static final VaultContainer vaultContainer = new VaultContainer<>("hashicorp/vault:1.13.3")
        .withVaultToken(VAULT_TOKEN)
        .dependsOn(kafka);

    @AfterAll
    static void cleanup() {
        vaultContainer.close();
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        super.configurePolicies(policies);

        policies.put(
            "assign-attributes",
            PolicyBuilder.build("assign-attributes", AssignAttributesPolicy.class, AssignAttributesPolicyConfiguration.class)
        );
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        super.configureGateway(configurationBuilder);

        // create a renewable token so the plugin does not start panicking
        Container.ExecResult execResult;
        try {
            execResult = vaultContainer.execInContainer("vault", "token", "create", "-period=10m", "-field", "token");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        String token = execResult.getStdout();

        configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "vault");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.enabled", true);
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.host", vaultContainer.getHost());
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.port", vaultContainer.getMappedPort(8200));
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.enabled", "false");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.auth.method", "token");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.auth.config.token", token);

        try {
            vaultContainer.execInContainer("vault", "kv", "put", "secret/kafka", "bootstrap=" + kafka.getBootstrapServers());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configureSecretProviders(
        Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
    ) {
        secretProviderPlugins.add(
            SecretProviderBuilder.build(HCVaultSecretProvider.PLUGIN_ID, HCVaultSecretProviderFactory.class, VaultConfig.class)
        );
    }

    @Override
    public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
        super.configureServices(services);
        services.add(SecretsService.class);
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint.json" })
    void should_receive_all_messages(HttpClient client, Vertx vertx) {
        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection.
        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "message1")
                    .andThen(publishToKafka(producer, "message2"))
                    .andThen(publishToKafka(producer, "message3"))
                    .doFinally(producer::close)
            )
            .blockingAwait();

        // First request should receive 2 first messages
        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(2);
                final JsonObject message = items.getJsonObject(0);
                assertThat(message.getString("id")).isNull();
                assertThat(message.getString("content")).isEqualTo("message1");
                final JsonObject message2 = items.getJsonObject(1);
                assertThat(message2.getString("id")).isNull();
                assertThat(message2.getString("content")).isEqualTo("message2");
                return true;
            });

        // Second request should receive last messages
        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(1);
                final JsonObject message = items.getJsonObject(0);
                assertThat(message.getString("id")).isNull();
                assertThat(message.getString("content")).isEqualTo("message3");
                return true;
            });

        // Third request should not receive anything
        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).isEmpty();
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-override-topic.json" })
    void should_receive_all_messages_from_topic_selected_by_attribute(HttpClient client, Vertx vertx) {
        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection.
        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "message1").andThen(publishToKafka(producer, "message2")).doFinally(producer::close)
            )
            .blockingAwait();

        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer -> publishToKafka(producer, "test-topic-attribute", "another-message").doFinally(producer::close))
            .blockingAwait();

        // First request should receive one message from test-topic-attribute. This topic is selected with Assign Attribute policy based on path params
        client
            .rxRequest(HttpMethod.GET, "/test/test-topic-attribute")
            .flatMap(request -> {
                // Override topic from header: topic is set on request phase
                request.putHeader("X-Topic-Override-Request-Level", "test-topic,test-topic-attribute");
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(3);
                final JsonObject message1 = items.getJsonObject(0);
                assertThat(message1.getString("id")).isNull();
                assertThat(message1.getJsonObject("metadata").getString("topic")).isEqualTo(TEST_TOPIC);
                assertThat(message1.getString("content")).isEqualTo("message1");
                final JsonObject message2 = items.getJsonObject(1);
                assertThat(message2.getString("id")).isNull();
                assertThat(message2.getJsonObject("metadata").getString("topic")).isEqualTo(TEST_TOPIC);
                assertThat(message2.getString("content")).isEqualTo("message2");
                final JsonObject messageOtherTopic = items.getJsonObject(2);
                assertThat(messageOtherTopic.getJsonObject("metadata").getString("topic")).isEqualTo("test-topic-attribute");
                assertThat(messageOtherTopic.getString("id")).isNull();
                assertThat(messageOtherTopic.getString("content")).isEqualTo("another-message");
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-dynamic-configuration.json" })
    void should_receive_all_messages_from_topic_selected_by_dynamic_configuration_and_override_auto_offset_reset(
        HttpClient client,
        Vertx vertx
    ) {
        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "message1").andThen(publishToKafka(producer, "message2")).doFinally(producer::close)
            )
            .blockingAwait();

        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "test-topic-dynamic-configuration", "another-message").doFinally(producer::close)
            )
            .blockingAwait();

        // First request should receive one message from test-topic-attribute. This topic is selected using the dynamic configuration feature that allows to override any property by using an attribute.
        // The auto offset reset is set to earliest using the fact that the dynamic configuration allows to use EL for String property
        client
            .rxRequest(HttpMethod.GET, "/test/test-dynamic-configuration")
            .flatMap(request -> {
                // Override topic from header: topic is set on request phase
                request.putHeader("X-AutoOffsetReset", "earliest");
                request.putHeader("X-Topic-Override-Request-Level", "test-topic,test-topic-dynamic-configuration");
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(3);
                final JsonObject message1 = items.getJsonObject(0);
                assertThat(message1.getString("id")).isNull();
                assertThat(message1.getJsonObject("metadata").getString("topic")).isEqualTo(TEST_TOPIC);
                assertThat(message1.getString("content")).isEqualTo("message1");
                final JsonObject message2 = items.getJsonObject(1);
                assertThat(message2.getString("id")).isNull();
                assertThat(message2.getJsonObject("metadata").getString("topic")).isEqualTo(TEST_TOPIC);
                assertThat(message2.getString("content")).isEqualTo("message2");
                final JsonObject messageOtherTopic = items.getJsonObject(2);
                assertThat(messageOtherTopic.getJsonObject("metadata").getString("topic")).isEqualTo("test-topic-dynamic-configuration");
                assertThat(messageOtherTopic.getString("id")).isNull();
                assertThat(messageOtherTopic.getString("content")).isEqualTo("another-message");
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-dynamic-configuration.json" })
    void should_interrupt_when_constraint_violated_with_dynamic_configuration(HttpClient client, Vertx vertx) {
        // The auto offset reset is set to a value not authorized by the validation annotation used in the configuration bean by the dynamic configuration feature
        client
            .rxRequest(HttpMethod.GET, "/test/test-dynamic-configuration-validation-failure")
            .flatMap(request -> {
                // Override topic from header: topic is set on request phase
                request.putHeader("X-AutoOffsetReset", "constraint_violation");
                request.putHeader("X-Topic-Override-Request-Level", "test-topic");
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(500);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                assertThat(jsonResponse.getString("message")).isEqualTo("Invalid configuration");
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-secret.json" })
    void should_receive_all_messages_with_bootstrap_servers_from_vault_secret(HttpClient client, Vertx vertx) {
        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "message1").andThen(publishToKafka(producer, "message2")).doFinally(producer::close)
            )
            .blockingAwait();

        client
            .rxRequest(HttpMethod.GET, "/test/test-secret")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(2);
                final JsonObject message1 = items.getJsonObject(0);
                assertThat(message1.getString("id")).isNull();
                assertThat(message1.getJsonObject("metadata").getString("topic")).isEqualTo(TEST_TOPIC);
                assertThat(message1.getString("content")).isEqualTo("message1");
                final JsonObject message2 = items.getJsonObject(1);
                assertThat(message2.getString("id")).isNull();
                assertThat(message2.getJsonObject("metadata").getString("topic")).isEqualTo(TEST_TOPIC);
                assertThat(message2.getString("content")).isEqualTo("message2");
                return true;
            });
    }

    @EnumSource(value = Qos.class, names = { "AT_MOST_ONCE", "AT_LEAST_ONCE" })
    @ParameterizedTest(name = "should receive all messages with {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-at-least-once.json",
            "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-at-most-once.json",
        }
    )
    void should_receive_all_messages_with_qos(Qos qos, HttpClient client, Vertx vertx) {
        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection through SSE entrypoint.
        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "message1")
                    .andThen(publishToKafka(producer, "message2"))
                    .andThen(publishToKafka(producer, "message3"))
                    .doFinally(producer::close)
            )
            .blockingAwait();

        // First request should receive 2 first messages
        client
            .rxRequest(HttpMethod.GET, "/test-" + qos.getLabel())
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(2);
                final JsonObject message = items.getJsonObject(0);
                assertThat(message.getString("content")).isEqualTo("message1");
                assertThat(message.getString("id")).isEqualTo("dGVzdC10b3BpY0AwIzA="); // test-topic@0#0
                final JsonObject message2 = items.getJsonObject(1);
                assertThat(message2.getString("content")).isEqualTo("message2");
                assertThat(message2.getString("id")).isEqualTo("dGVzdC10b3BpY0AwIzE="); // test-topic@0#1

                final JsonObject pagination = jsonResponse.getJsonObject("pagination");
                assertThat(pagination.getString("nextCursor")).isEqualTo("dGVzdC10b3BpY0AwIzE="); // test-topic@0#1

                return true;
            });

        // Second request should receive last messages
        client
            .rxRequest(HttpMethod.GET, "/test-" + qos.getLabel())
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(1);
                final JsonObject message3 = items.getJsonObject(0);
                assertThat(message3.getString("content")).isEqualTo("message3");
                assertThat(message3.getString("id")).isEqualTo("dGVzdC10b3BpY0AwIzI="); // test-topic@0#2

                final JsonObject pagination = jsonResponse.getJsonObject("pagination");
                assertThat(pagination.getString("nextCursor")).isEqualTo("dGVzdC10b3BpY0AwIzI="); // test-topic@0#2

                return true;
            });

        // Third request should not receive anything
        client
            .rxRequest(HttpMethod.GET, "/test-" + qos.getLabel())
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).isEmpty();

                final JsonObject pagination = jsonResponse.getJsonObject("pagination");
                assertThat(pagination.getString("nextCursor")).isEqualTo("dGVzdC10b3BpY0AwIzI="); // test-topic@0#1

                return true;
            });
    }

    @EnumSource(value = Qos.class, names = { "AT_MOST_ONCE", "AT_LEAST_ONCE" })
    @ParameterizedTest(name = "should receive empty items while requesting recent cursor with {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-at-least-once.json",
            "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-at-most-once.json",
        }
    )
    void should_receive_empty_items_while_requesting_recent_cursor_with_specific_qos(Qos qos, HttpClient client, Vertx vertx) {
        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection through SSE entrypoint.
        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "message1")
                    .andThen(publishToKafka(producer, "message2"))
                    .andThen(publishToKafka(producer, "message3"))
                    .doFinally(producer::close)
            )
            .blockingAwait();

        // cursor=test-topic@0#2
        client
            .rxRequest(HttpMethod.GET, "/test-" + qos.getLabel() + "?cursor=dGVzdC10b3BpY0AwIzI=")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).isEmpty();

                final JsonObject pagination = jsonResponse.getJsonObject("pagination");
                assertThat(pagination.getString("nextCursor")).isEqualTo("dGVzdC10b3BpY0AwIzI="); // test-topic@0#2

                return true;
            });
    }

    @EnumSource(value = Qos.class, names = { "AT_MOST_ONCE", "AT_LEAST_ONCE" })
    @ParameterizedTest(name = "should receive empty items with_initial_cursor with_no_messages and {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-at-least-once.json",
            "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-at-most-once.json",
        }
    )
    void should_receive_empty_items_with_initial_cursor_with_no_messages_and_specific_qos(Qos qos, HttpClient client) {
        client
            .rxRequest(HttpMethod.GET, "/test-" + qos.getLabel())
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).isEmpty();

                final JsonObject pagination = jsonResponse.getJsonObject("pagination");
                assertThat(pagination.getString("nextCursor")).isEqualTo("dGVzdC10b3BpY0AwIzA="); // test-topic@0#2

                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-failure.json" })
    void should_receive_error_messages_when_error_occurred(HttpClient client) {
        // First request should receive 2 first messages
        client
            .rxRequest(HttpMethod.GET, "/test-failure")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).isEmpty();

                final JsonObject error = jsonResponse.getJsonObject("error");
                assertThat(error.getString("id")).isNotNull();
                final JsonObject metadata = error.getJsonObject("metadata");
                assertThat(metadata.getString("key")).isEqualTo("FAILURE_ENDPOINT_CONFIGURATION_INVALID");

                return true;
            });
    }
}
