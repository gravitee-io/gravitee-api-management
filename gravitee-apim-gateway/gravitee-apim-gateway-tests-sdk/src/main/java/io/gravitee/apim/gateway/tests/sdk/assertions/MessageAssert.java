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
package io.gravitee.apim.gateway.tests.sdk.assertions;

import io.gravitee.gateway.reactive.api.message.Message;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class MessageAssert extends AbstractAssert<MessageAssert, Message> {

    public MessageAssert(Message message) {
        super(message, MessageAssert.class);
    }

    public static MessageAssert assertThat(Message actual) {
        return new MessageAssert(actual);
    }

    public MessageAssert hasContent(String body) {
        isNotNull();
        String message = "\nExpecting content to be:\n  <%s>\nbut was:\n  <%s>";

        if (!actual.content().toString().equals(body)) {
            failWithMessage(message, body, actual.content());
        }
        return this;
    }

    public MessageAssert hasCorrelationId(String correlationId) {
        isNotNull();

        return hasNullableProperties(actual.correlationId(), correlationId, "\nExpecting CorrelationId to be:\n  <%s>\nbut was:\n  <%s>");
    }

    public MessageAssert hasId(String id) {
        isNotNull();

        return hasNullableProperties(actual.id(), id, "\nExpecting Id to be:\n  <%s>\nbut was:\n  <%s>");
    }

    public MessageAssert hasHeaders(Map<String, Object> expectedHeaders) {
        var actualHeaders = new HashMap<String, Object>();
        actual
            .headers()
            .iterator()
            .forEachRemaining(h -> actualHeaders.put(h.getKey(), h.getValue()));

        Assertions.assertThat(actualHeaders)
            .describedAs("\nExpecting headers to contains:\n  <%s>\nbut was:\n  <%s>", expectedHeaders, actualHeaders)
            .containsAllEntriesOf(expectedHeaders);
        return this;
    }

    public MessageAssert hasMetadata(Map<String, Object> expectedMetadata) {
        var actualMetadata = actual.metadata();

        Assertions.assertThat(actual.metadata())
            .describedAs("\nExpecting metadata to contains:\n  <%s>\nbut was:\n  <%s>", expectedMetadata, actualMetadata)
            .containsAllEntriesOf(expectedMetadata);
        return this;
    }

    public MessageAssert hasOnlyMetadataKey(List<String> expectedMetadataKey) {
        var actualMetadata = actual.metadata();

        Assertions.assertThat(actual.metadata())
            .describedAs(
                "\nExpecting metadata to contains only the keys:\n  <%s>\nbut was:\n  <%s>",
                expectedMetadataKey,
                actualMetadata.keySet()
            )
            .containsOnlyKeys(expectedMetadataKey);
        return this;
    }

    private MessageAssert hasNullableProperties(String actualProperty, String expectedProperty, String failureMessage) {
        if (actualProperty == null) {
            if (expectedProperty != null) {
                failWithMessage(failureMessage, expectedProperty, actualProperty);
            }
            return this;
        }

        if (!actualProperty.equals(expectedProperty)) {
            failWithMessage(failureMessage, expectedProperty, actualProperty);
        }
        return this;
    }
}
