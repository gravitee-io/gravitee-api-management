/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.messages.sse;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.vertx.rxjava3.core.buffer.Buffer;
import jakarta.validation.constraints.NotNull;
import java.util.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AbstractSseGatewayTest extends AbstractGatewayTest {

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
    }

    protected void assertRetry(Buffer chunk) {
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(1);
        assertThat(splitMessage[0]).startsWith("retry: ");
    }

    protected void assertOnMessage(Buffer chunk, String messageContent) {
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(2);
        assertMessageData(messageContent, splitMessage);
    }

    protected void assertOnMessage(Buffer chunk, long id, String messageContent) {
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(3);
        assertMessageData(messageContent, id, splitMessage);
    }

    protected void assertOnMessage(Buffer chunk, long id, String messageContent, @NotNull String... expectedComments) {
        Objects.requireNonNull(expectedComments);
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(3 + expectedComments.length);
        assertMessageData(messageContent, id, splitMessage);
        List<String> actualComments = new ArrayList<>();
        for (int i = 3; i < splitMessage.length; i++) {
            actualComments.add(splitMessage[i].substring(1)); // remove starting ':'
        }
        assertThat(actualComments).containsExactlyInAnyOrder(expectedComments);
    }

    private static void assertMessageData(String messageContent, String[] splitMessage) {
        assertThat(splitMessage[0]).isEqualTo("event: message");
        assertThat(splitMessage[1]).isEqualTo("data: " + messageContent);
    }

    private static void assertMessageData(String messageContent, long id, String[] splitMessage) {
        assertThat(splitMessage[0]).isEqualTo("id: " + id);
        assertThat(splitMessage[1]).isEqualTo("event: message");
        assertThat(splitMessage[2]).isEqualTo("data: " + messageContent);
    }
}
