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
package io.gravitee.apim.integration.tests.messages.sse;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.rxjava3.core.buffer.Buffer;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
class SseAssertions {

    private SseAssertions() {
        // no op
    }

    static void assertRetry(Buffer chunk) {
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(1);
        assertThat(splitMessage[0]).startsWith("retry: ");
    }

    static void assertOnMessage(Buffer chunk, String messageContent) {
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(2);
        assertMessageData(messageContent, splitMessage);
    }

    static void assertOnMessage(Buffer chunk, long id, String messageContent) {
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(3);
        assertMessageData(messageContent, id, splitMessage);
    }

    static void assertOnMessage(Buffer chunk, long id, String messageContent, @NotNull String... expectedComments) {
        Objects.requireNonNull(expectedComments);
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(4 + expectedComments.length); // 3 message data + 1 sourceTimestamp + n expected comments
        assertMessageData(messageContent, id, splitMessage);

        // check that sourceTimestamp is filled
        assertThat(splitMessage[3]).matches(":sourceTimestamp: \\d+");

        List<String> actualComments = new ArrayList<>();
        for (int i = 4; i < splitMessage.length; i++) {
            actualComments.add(splitMessage[i].substring(1)); // remove starting ':'
        }
        assertThat(actualComments).containsExactlyInAnyOrder(expectedComments);
    }

    static void assertHeartbeat(Buffer chunk) {
        assertThat(chunk.toString()).isEqualTo(":\n\n");
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
