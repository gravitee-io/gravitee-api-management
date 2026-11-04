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
package io.gravitee.apim.integration.tests.messages.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.websocket.WebSocketCloseStatus;
import com.graviteesource.entrypoint.websocket.WebSocketEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.fake.InvalidUtf8MessagePolicy;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.rxjava3.core.http.WebSocket;
import io.vertx.rxjava3.core.http.WebSocketClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebsocketEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    private static final int MESSAGE_COUNT = 5;
    private static final String MESSAGE = "message";
    private static final String MULTIBYTE_MESSAGE = "zażółć € 😀";
    private static final int FRAMES_PER_CONNECTION = 3;

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("websocket", EntrypointBuilder.build("websocket", WebSocketEntrypointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put(InvalidUtf8MessagePolicy.ID, PolicyBuilder.build(InvalidUtf8MessagePolicy.ID, InvalidUtf8MessagePolicy.class));
    }

    @EnumSource(value = Qos.class, names = { "NONE", "AUTO" })
    @ParameterizedTest(name = "should send text frames with {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-text-none.json",
            "/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-text-auto.json",
        }
    )
    void should_send_text_frames_when_frame_type_is_text(Qos qos, WebSocketClient client) {
        assertTextFrames(receive(client, "/text-" + qos.getLabel(), MESSAGE_COUNT), MESSAGE);
    }

    @EnumSource(value = Qos.class, names = { "NONE", "AUTO" })
    @ParameterizedTest(name = "should send binary frames with {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-binary-none.json",
            "/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-binary-auto.json",
        }
    )
    void should_send_binary_frames_when_frame_type_is_binary(Qos qos, WebSocketClient client) {
        assertBinaryFrames(receive(client, "/binary-" + qos.getLabel(), MESSAGE_COUNT));
    }

    @EnumSource(value = Qos.class, names = { "NONE", "AUTO" })
    @ParameterizedTest(name = "should send binary frames by default with {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-unset-none.json",
            "/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-unset-auto.json",
        }
    )
    void should_send_binary_frames_when_frame_type_is_unset(Qos qos, WebSocketClient client) {
        assertBinaryFrames(receive(client, "/unset-" + qos.getLabel(), MESSAGE_COUNT));
    }

    @Test
    @DeployApi("/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-text-multibyte.json")
    void should_keep_multibyte_characters_intact_in_text_frames(WebSocketClient client) {
        assertTextFrames(receive(client, "/text-multibyte", MESSAGE_COUNT), MULTIBYTE_MESSAGE);
    }

    @EnumSource(value = Qos.class, names = { "NONE", "AUTO" })
    @ParameterizedTest(name = "should close with invalid payload data with {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-text-invalid-utf8-none.json",
            "/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-text-invalid-utf8-auto.json",
        }
    )
    void should_close_with_invalid_payload_data_when_text_message_is_not_utf8(Qos qos, WebSocketClient client) {
        AtomicReference<WebSocket> webSocket = new AtomicReference<>();

        List<WebSocketFrame> frames = client
            .rxConnect("/text-invalid-utf8-" + qos.getLabel())
            .doOnSuccess(webSocket::set)
            .flatMapPublisher(WebsocketEntrypointMockEndpointIntegrationTest::dataFrames)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .values();

        assertThat(frames).hasSize(2);
        assertTextFrames(frames, MESSAGE);
        assertThat(webSocket.get().closeStatusCode()).isEqualTo((short) WebSocketCloseStatus.INVALID_PAYLOAD_DATA.code());
    }

    @Test
    @DeployApi("/apis/v4/messages/websocket/websocket-entrypoint-mock-endpoint-text-endless.json")
    void should_serve_a_new_text_connection_after_a_client_disconnect(WebSocketClient client) {
        client
            .rxConnect("/text-endless")
            .flatMapCompletable(webSocket ->
                dataFrames(webSocket).take(FRAMES_PER_CONNECTION).ignoreElements().andThen(webSocket.rxClose())
            )
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete();

        assertTextFrames(receive(client, "/text-endless", FRAMES_PER_CONNECTION), MESSAGE);
    }

    private static List<WebSocketFrame> receive(WebSocketClient client, String path, int frameCount) {
        return client
            .rxConnect(path)
            .flatMapPublisher(WebsocketEntrypointMockEndpointIntegrationTest::dataFrames)
            .take(frameCount)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(frameCount)
            .values();
    }

    private static void assertTextFrames(List<WebSocketFrame> frames, String expectedContent) {
        assertThat(frames).allSatisfy(frame -> {
            assertThat(frame.isText()).isTrue();
            assertThat(frame.textData()).isEqualTo(expectedContent);
        });
    }

    private static void assertBinaryFrames(List<WebSocketFrame> frames) {
        assertThat(frames).allSatisfy(frame -> {
            assertThat(frame.isBinary()).isTrue();
            assertThat(frame.binaryData()).hasToString(MESSAGE);
        });
    }

    /**
     * {@link WebSocket#toFlowable()} hides whether a frame was text or binary, so the frames are read from the frame handler instead.
     * Don't combine both on the same socket.
     */
    private static Flowable<WebSocketFrame> dataFrames(WebSocket webSocket) {
        return Flowable.create(
            emitter -> {
                webSocket.frameHandler(frame -> {
                    if (frame.isText() || frame.isBinary() || frame.isContinuation()) {
                        emitter.onNext(frame);
                    }
                });
                webSocket.closeHandler(ignored -> emitter.onComplete());
                webSocket.exceptionHandler(emitter::onError);
            },
            BackpressureStrategy.BUFFER
        );
    }
}
