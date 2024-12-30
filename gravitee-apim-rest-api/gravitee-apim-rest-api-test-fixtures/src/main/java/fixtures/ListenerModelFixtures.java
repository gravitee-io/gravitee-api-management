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
package fixtures;

import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.List;
import java.util.Set;

public class ListenerModelFixtures {

    private ListenerModelFixtures() {}

    private static final Path.PathBuilder BASE_MODEL_PATH_V4 = Path.builder().host("my.fake.host").path("/test").overrideAccess(true);

    private static final io.gravitee.definition.model.v4.listener.http.HttpListener.HttpListenerBuilder<?, ?> BASE_MODEL_HTTP_LISTENER =
        io.gravitee.definition.model.v4.listener.http.HttpListener
            .builder()
            // Listener
            .entrypoints(List.of(EntrypointModelFixtures.aModelEntrypointHttpV4()))
            .servers(List.of("my-server1", "my-server2"))
            // HttpListener specific
            .paths(List.of(BASE_MODEL_PATH_V4.build()))
            .pathMappings(Set.of("/test"))
            .cors(CorsModelFixtures.aModelCors());

    private static final io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener.SubscriptionListenerBuilder<?, ?> BASE_MODEL_SUBSCRIPTION_LISTENER =
        io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener
            .builder()
            // BaseListener
            .entrypoints(List.of(EntrypointModelFixtures.aModelEntrypointHttpV4()))
            .servers(List.of("my-server1", "my-server2"));

    private static final io.gravitee.definition.model.v4.listener.tcp.TcpListener.TcpListenerBuilder<?, ?> BASE_MODEL_TCP_LISTENER =
        io.gravitee.definition.model.v4.listener.tcp.TcpListener
            .builder()
            // BaseListener
            .hosts(List.of("fake.host.io"))
            .entrypoints(List.of(EntrypointModelFixtures.aModelEntrypointHttpV4()))
            .servers(List.of("my-server1", "my-server2"));

    private static final io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener.KafkaListenerBuilder<?, ?> BASE_MODEL_KAFKA_LISTENER =
        io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener
            .builder()
            // BaseListener
            .host("fake.host.io")
            .port(1000)
            .entrypoints(List.of(EntrypointModelFixtures.aModelEntrypointNativeV4()))
            .servers(List.of("my-server1", "my-server2"));

    public static io.gravitee.definition.model.v4.listener.http.HttpListener aModelHttpListener() {
        return BASE_MODEL_HTTP_LISTENER.build();
    }

    public static io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener aModelSubscriptionListener() {
        return BASE_MODEL_SUBSCRIPTION_LISTENER.build();
    }

    public static io.gravitee.definition.model.v4.listener.tcp.TcpListener aModelTcpListener() {
        return BASE_MODEL_TCP_LISTENER.build();
    }

    public static io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener aModelKafkaListener() {
        return BASE_MODEL_KAFKA_LISTENER.build();
    }
}
