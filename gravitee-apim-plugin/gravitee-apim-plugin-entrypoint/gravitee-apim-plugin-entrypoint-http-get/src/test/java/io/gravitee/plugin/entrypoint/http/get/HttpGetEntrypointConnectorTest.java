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
package io.gravitee.plugin.entrypoint.http.get;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.plugin.entrypoint.http.get.configuration.HttpGetEntrypointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpGetEntrypointConnectorTest {

    Logger logger = LoggerFactory.getLogger(HttpGetEntrypointConnectorTest.class);

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Spy
    private HttpGetEntrypointConnectorConfiguration configuration = new HttpGetEntrypointConnectorConfiguration();

    @Captor
    private ArgumentCaptor<Flowable<Buffer>> chunksCaptor;

    private HttpGetEntrypointConnector cut;

    @BeforeEach
    void beforeEach() {
        cut = new HttpGetEntrypointConnector(Qos.NONE, configuration);
        lenient().when(configuration.isHeadersInPayload()).thenReturn(true);
        lenient().when(configuration.isMetadataInPayload()).thenReturn(true);
    }

    @Test
    void shouldIdReturnHttpGet() {
        assertThat(cut.id()).isEqualTo("http-get");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportHttpListener() {
        assertThat(cut.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldSupportSubscribeModeOnly() {
        assertThat(cut.supportedModes()).containsOnly(ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(cut.matchCriteriaCount()).isEqualTo(1);
    }

    @Test
    void shouldMatchesWithValidContext() {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(ctx.request()).thenReturn(request);

        boolean matches = cut.matches(ctx);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithBadMethod() {
        when(request.method()).thenReturn(HttpMethod.POST);
        when(ctx.request()).thenReturn(request);

        boolean matches = cut.matches(ctx);

        assertThat(matches).isFalse();
    }

    @ParameterizedTest
    @MethodSource("io.gravitee.plugin.entrypoint.http.get.utils.IntegrationTestMethodSourceProvider#provideBadAcceptHeaders")
    void shouldInterruptWithFailureBadAcceptHeader(List<String> acceptHeaderValues) {
        final HttpHeaders requestHttpHeaders = HttpHeaders.create();
        // add multiple ACCEPT headers depending on the parameter
        for (String header : acceptHeaderValues) {
            requestHttpHeaders.add(HttpHeaderNames.ACCEPT, header);
        }
        when(request.headers()).thenReturn(requestHttpHeaders);

        when(ctx.request()).thenReturn(request);

        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        cut
            .handleRequest(ctx)
            .test()
            .assertError(
                e -> {
                    assertTrue(e instanceof InterruptionFailureException);
                    assertEquals(HttpStatusCode.BAD_REQUEST_400, ((InterruptionFailureException) e).getExecutionFailure().statusCode());
                    assertEquals(
                        "Unsupported accept header: " + requestHttpHeaders.getAll(HttpHeaderNames.ACCEPT),
                        ((InterruptionFailureException) e).getExecutionFailure().message()
                    );
                    return true;
                }
            );
    }

    @ParameterizedTest(name = "Expected: {0}, parameters: {1}")
    @DisplayName("Should select the best Content-Type based on ACCEPT headers, including quality parameter")
    @MethodSource("io.gravitee.plugin.entrypoint.http.get.utils.IntegrationTestMethodSourceProvider#provideValidAcceptHeaders")
    void shouldSelectTheBestContentTypeBasedOnAcceptHeader(String expectedHeader, List<String> acceptHeaderValues)
        throws InterruptedException {
        final HttpHeaders requestHttpHeaders = HttpHeaders.create();
        // add multiple ACCEPT headers depending on the parameter
        for (String header : acceptHeaderValues) {
            requestHttpHeaders.add(HttpHeaderNames.ACCEPT, header);
        }
        when(request.headers()).thenReturn(requestHttpHeaders);
        when(request.parameters()).thenReturn(new LinkedMultiValueMap<>());

        when(ctx.request()).thenReturn(request);

        cut.handleRequest(ctx).test().await().assertComplete();

        verify(ctx, times(1)).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE, expectedHeader);
    }

    @Test
    @DisplayName("Should select the best Content-Type based on ACCEPT headers, including quality parameter")
    void shouldSelectTextPlainWhenAcceptHeaderNull() throws InterruptedException {
        final HttpHeaders requestHttpHeaders = HttpHeaders.create();
        when(request.headers()).thenReturn(requestHttpHeaders);
        when(request.parameters()).thenReturn(new LinkedMultiValueMap<>());

        when(ctx.request()).thenReturn(request);

        cut.handleRequest(ctx).test().await().assertComplete();

        verify(ctx, times(1)).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE, MediaType.TEXT_PLAIN);
    }

    @ParameterizedTest
    @CsvSource({ "0,2", "1,2", "6,2" })
    void shouldSetupInternalAttributeWithConfigurationValuesOnRequest(String limitQueryParam, int messagesLimitCount) {
        final HttpHeaders requestHttpHeaders = HttpHeaders.create();
        requestHttpHeaders.add(HttpHeaderNames.ACCEPT, MediaType.WILDCARD);
        when(request.headers()).thenReturn(requestHttpHeaders);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(HttpGetEntrypointConnector.CURSOR_QUERY_PARAM, "1234");
        queryParams.add(HttpGetEntrypointConnector.LIMIT_QUERY_PARAM, limitQueryParam);
        when(request.parameters()).thenReturn(queryParams);
        when(ctx.request()).thenReturn(request);
        when(configuration.getMessagesLimitCount()).thenReturn(messagesLimitCount);
        when(configuration.getMessagesLimitDurationMs()).thenReturn(1_000L);
        cut.handleRequest(ctx).test().assertComplete();

        verify(ctx)
            .putInternalAttribute(
                InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT,
                Math.min(messagesLimitCount, Integer.parseInt(limitQueryParam))
            );
        verify(ctx).putInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS, 1_000L);
        verify(ctx).putInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RECOVERY_LAST_ID, "1234");
        verify(ctx).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    @Test
    void shouldCompleteAndEndWhenResponseMessagesComplete() {
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE))
            .thenReturn(MediaType.APPLICATION_JSON);

        final HttpHeaders httpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(httpHeaders);
        Flowable<Message> empty = Flowable.empty();
        when(response.messages()).thenReturn(empty);
        when(ctx.response()).thenReturn(response);

        cut.handleResponse(ctx).test().assertComplete();

        assertThat(httpHeaders).isNotNull();
        assertThat(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

        chunkObs
            .assertComplete()
            .assertValueCount(3)
            .assertValueAt(0, message -> message.toString().equals("{\"items\":["))
            .assertValueAt(1, message -> message.toString().equals("]"))
            .assertValueAt(2, message -> message.toString().equals("}"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldJsonArrayOfMessageAndCompleteAndEndWhenResponseMessagesComplete(boolean withHeadersAndMetadata) throws InterruptedException {
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE))
            .thenReturn(MediaType.APPLICATION_JSON);
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID)).thenReturn("2");
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(30_000L);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(2);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(HttpGetEntrypointConnector.CURSOR_QUERY_PARAM, "0");
        queryParams.add(HttpGetEntrypointConnector.LIMIT_QUERY_PARAM, "2");
        when(request.parameters()).thenReturn(queryParams);
        when(ctx.request()).thenReturn(request);

        final HttpHeaders httpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(httpHeaders);
        Flowable<Message> messages = Flowable.just(
            createMessage("1", MediaType.APPLICATION_JSON),
            createMessage("2", null),
            createMessage("Non returned messaged due to count limit", null)
        );
        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        when(configuration.isHeadersInPayload()).thenReturn(withHeadersAndMetadata);
        when(configuration.isMetadataInPayload()).thenReturn(withHeadersAndMetadata);

        cut.handleResponse(ctx).test().assertComplete();

        assertThat(httpHeaders).isNotNull();
        assertThat(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

        chunkObs
            .await()
            .assertComplete()
            .assertValueCount(6)
            .assertValueAt(
                0,
                message -> {
                    logger.debug("message[0]: {}", message.toString());
                    return message.toString().equals("{\"items\":[");
                }
            )
            .assertValueAt(
                1,
                message -> {
                    logger.debug("message[1] - {}: {}", withHeadersAndMetadata, message.toString());
                    if (withHeadersAndMetadata) {
                        assertThat(message.toString())
                            .startsWith(
                                "{\"id\":\"1\",\"content\":\"{\\\"foo\\\": \\\"1\\\"}\",\"headers\":{\"X-My-Header-1\":[\"headerValue1\"]},\"metadata\":{\"sourceTimestamp\":"
                            )
                            .endsWith("\"myKey\":\"myValue1\"}}");
                    } else {
                        assertThat(message).hasToString("{\"id\":\"1\",\"content\":\"{\\\"foo\\\": \\\"1\\\"}\"}");
                    }
                    return true;
                }
            )
            .assertValueAt(
                2,
                message -> {
                    logger.debug("message[2] - {}: {}", withHeadersAndMetadata, message.toString());
                    if (withHeadersAndMetadata) {
                        assertThat(message.toString())
                            .startsWith(
                                ",{\"id\":\"2\",\"content\":\"2\",\"headers\":{\"X-My-Header-2\":[\"headerValue2\"]},\"metadata\":{\"sourceTimestamp\":"
                            )
                            .endsWith("\"myKey\":\"myValue2\"}}");
                    } else {
                        assertThat(message).hasToString(",{\"id\":\"2\",\"content\":\"2\"}");
                    }
                    return true;
                }
            )
            .assertValueAt(
                3,
                message -> {
                    logger.debug("message[3]: {}", message.toString());
                    return message.toString().equals("]");
                }
            )
            .assertValueAt(
                4,
                message -> {
                    logger.debug("message[4]: {}", message.toString());
                    return message.toString().equals(",\"pagination\":{\"nextCursor\":\"2\",\"limit\":\"2\"}");
                }
            )
            .assertValueAt(
                5,
                message -> {
                    logger.debug("message[5]: {}", message.toString());
                    return message.toString().equals("}");
                }
            );

        verify(ctx).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID, "2");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldXmlArrayOfMessageAndCompleteAndEndWhenResponseMessagesComplete(boolean withHeadersAndMetadata) throws InterruptedException {
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE))
            .thenReturn(MediaType.APPLICATION_XML);
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID)).thenReturn("2");
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(5_000L);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(2);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(HttpGetEntrypointConnector.CURSOR_QUERY_PARAM, "0");
        queryParams.add(HttpGetEntrypointConnector.LIMIT_QUERY_PARAM, "2");
        when(request.parameters()).thenReturn(queryParams);
        when(ctx.request()).thenReturn(request);

        final HttpHeaders responseHttpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHttpHeaders);
        Flowable<Message> messages = Flowable.just(
            createMessage("1", MediaType.APPLICATION_XML),
            createMessage("2", null),
            createMessage("Non returned messaged due to count limit", null)
        );
        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        when(configuration.isHeadersInPayload()).thenReturn(withHeadersAndMetadata);
        when(configuration.isMetadataInPayload()).thenReturn(withHeadersAndMetadata);

        cut.handleResponse(ctx).test().assertComplete();

        assertThat(responseHttpHeaders).isNotNull();
        assertThat(responseHttpHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_XML);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

        chunkObs
            .await()
            .assertComplete()
            .assertValueCount(6)
            .assertValueAt(0, message -> message.toString().equals("<response><items>"))
            .assertValueAt(
                1,
                message -> {
                    if (withHeadersAndMetadata) {
                        assertThat(message.toString())
                            .startsWith(
                                "<item><id>1</id><content><![CDATA[<foo>1</foo>]]></content><headers><X-My-Header-1>headerValue1</X-My-Header-1></headers><metadata><sourceTimestamp>"
                            )
                            .endsWith("</sourceTimestamp><myKey>myValue1</myKey></metadata></item>");
                    } else {
                        assertThat(message).hasToString("<item><id>1</id><content><![CDATA[<foo>1</foo>]]></content></item>");
                    }
                    return true;
                }
            )
            .assertValueAt(
                2,
                message -> {
                    if (withHeadersAndMetadata) {
                        assertThat(message.toString())
                            .startsWith(
                                "<item><id>2</id><content><![CDATA[2]]></content><headers><X-My-Header-2>headerValue2</X-My-Header-2></headers><metadata><sourceTimestamp>"
                            )
                            .endsWith("</sourceTimestamp><myKey>myValue2</myKey></metadata></item>");
                    } else {
                        assertThat(message).hasToString("<item><id>2</id><content><![CDATA[2]]></content></item>");
                    }
                    return true;
                }
            )
            .assertValueAt(3, message -> message.toString().equals("</items>"))
            .assertValueAt(4, message -> message.toString().equals("<pagination><nextCursor>2</nextCursor><limit>2</limit></pagination>"))
            .assertValueAt(5, message -> message.toString().equals("</response>"));

        verify(ctx).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID, "2");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldTextPlainArrayOfMessageAndCompleteAndEndWhenResponseMessagesComplete(boolean withHeadersAndMetadata)
        throws InterruptedException {
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE)).thenReturn(MediaType.TEXT_PLAIN);
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID)).thenReturn("2");
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(5_000L);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(2);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(HttpGetEntrypointConnector.CURSOR_QUERY_PARAM, "0");
        queryParams.add(HttpGetEntrypointConnector.LIMIT_QUERY_PARAM, "2");
        when(request.parameters()).thenReturn(queryParams);
        when(ctx.request()).thenReturn(request);

        final HttpHeaders responseHttpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHttpHeaders);
        Flowable<Message> messages = Flowable.just(
            createMessage("1", null),
            createMessage("2", null),
            createMessage("Non returned messaged due to count limit", null)
        );
        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        when(configuration.isHeadersInPayload()).thenReturn(withHeadersAndMetadata);
        when(configuration.isMetadataInPayload()).thenReturn(withHeadersAndMetadata);

        when(configuration.isHeadersInPayload()).thenReturn(withHeadersAndMetadata);
        when(configuration.isMetadataInPayload()).thenReturn(withHeadersAndMetadata);

        cut.handleResponse(ctx).test().assertComplete();

        assertThat(responseHttpHeaders).isNotNull();
        assertThat(responseHttpHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

        chunkObs
            .await()
            .assertComplete()
            .assertValueCount(4)
            .assertValueAt(0, message -> message.toString().equals("items\n"))
            .assertValueAt(
                1,
                message -> {
                    if (withHeadersAndMetadata) {
                        final Map<String, String> messageMap = messageToMap(message);
                        assertThat(messageMap)
                            .containsEntry("id", "1")
                            .containsEntry("content", "1")
                            .containsEntry("headers", "{X-My-Header-1=[headerValue1]}")
                            .hasEntrySatisfying(
                                "metadata",
                                value -> {
                                    assertThat(value).startsWith("{sourceTimestamp").endsWith("myKey=myValue1}");
                                }
                            );
                    } else {
                        assertThat(message).hasToString("item\n" + "id: 1\n" + "content: 1\n");
                    }
                    return true;
                }
            )
            .assertValueAt(
                2,
                message -> {
                    if (withHeadersAndMetadata) {
                        final Map<String, String> messageMap = messageToMap(message);
                        assertThat(messageMap)
                            .containsEntry("id", "2")
                            .containsEntry("content", "2")
                            .containsEntry("headers", "{X-My-Header-2=[headerValue2]}")
                            .hasEntrySatisfying(
                                "metadata",
                                value -> {
                                    assertThat(value).startsWith("{sourceTimestamp").endsWith("myKey=myValue2}");
                                }
                            );
                    } else {
                        assertThat(message).hasToString("\nitem\n" + "id: 2\n" + "content: 2\n");
                    }
                    return true;
                }
            )
            .assertValueAt(3, message -> message.toString().equals("\npagination\nnextCursor: 2\nlimit: 2"));

        verify(ctx).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID, "2");
    }

    private static Map<String, String> messageToMap(Buffer message) {
        return Arrays
            .stream(message.toString().split("\n"))
            .collect(Collectors.toMap(s -> s.split(":")[0], s -> s.contains(":") ? s.split(":")[1].trim() : ""));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldReturnJsonArrayOfMessageWithErrorWhenResponseMessagesContainErrorMessage(boolean withHeadersAndMetadata)
        throws InterruptedException {
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE))
            .thenReturn(MediaType.APPLICATION_JSON);
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID)).thenReturn("2");
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(5_000L);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(2);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(HttpGetEntrypointConnector.CURSOR_QUERY_PARAM, "0");
        queryParams.add(HttpGetEntrypointConnector.LIMIT_QUERY_PARAM, "2");
        when(request.parameters()).thenReturn(queryParams);
        when(ctx.request()).thenReturn(request);

        final HttpHeaders httpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(httpHeaders);
        Flowable<Message> messages = Flowable.just(
            createMessage("1", MediaType.APPLICATION_JSON),
            createMessage("2", null, true),
            createMessage("Non returned messaged due to count limit", null)
        );
        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        when(configuration.isHeadersInPayload()).thenReturn(withHeadersAndMetadata);
        when(configuration.isMetadataInPayload()).thenReturn(withHeadersAndMetadata);

        cut.handleResponse(ctx).test().assertComplete();

        assertThat(httpHeaders).isNotNull();
        assertThat(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

        chunkObs
            .await()
            .assertComplete()
            .assertValueCount(7)
            .assertValueAt(0, message -> message.toString().equals("{\"items\":["))
            .assertValueAt(
                1,
                message -> {
                    if (withHeadersAndMetadata) {
                        assertThat(message.toString())
                            .startsWith(
                                "{\"id\":\"1\",\"content\":\"{\\\"foo\\\": \\\"1\\\"}\",\"headers\":{\"X-My-Header-1\":[\"headerValue1\"]},\"metadata\":{\"sourceTimestamp\":"
                            )
                            .endsWith("\"myKey\":\"myValue1\"}}");
                    } else {
                        assertThat(message).hasToString("{\"id\":\"1\",\"content\":\"{\\\"foo\\\": \\\"1\\\"}\"}");
                    }
                    return true;
                }
            )
            .assertValueAt(2, message -> message.toString().equals("]"))
            .assertValueAt(3, message -> message.toString().equals(",\"error\":"))
            .assertValueAt(
                4,
                message -> {
                    if (withHeadersAndMetadata) {
                        assertThat(message.toString())
                            .startsWith(
                                "{\"id\":\"2\",\"content\":\"2\",\"headers\":{\"X-My-Header-2\":[\"headerValue2\"]},\"metadata\":{\"sourceTimestamp\":"
                            )
                            .endsWith("\"myKey\":\"myValue2\"}}");
                    } else {
                        assertThat(message).hasToString("{\"id\":\"2\",\"content\":\"2\"}");
                    }
                    return true;
                }
            )
            .assertValueAt(5, message -> message.toString().equals(",\"pagination\":{\"nextCursor\":\"2\",\"limit\":\"2\"}"))
            .assertValueAt(6, message -> message.toString().equals("}"));

        verify(ctx).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID, "1");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldReturnXmlArrayOfMessageWithErrorWhenResponseMessagesContainErrorMessage(boolean withHeadersAndMetadata)
        throws InterruptedException {
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE))
            .thenReturn(MediaType.APPLICATION_XML);
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID)).thenReturn("2");
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(5_000L);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(2);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(HttpGetEntrypointConnector.CURSOR_QUERY_PARAM, "0");
        queryParams.add(HttpGetEntrypointConnector.LIMIT_QUERY_PARAM, "2");
        when(request.parameters()).thenReturn(queryParams);
        when(ctx.request()).thenReturn(request);

        final HttpHeaders responseHttpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHttpHeaders);
        Flowable<Message> messages = Flowable.just(
            createMessage("1", MediaType.APPLICATION_XML),
            createMessage("2", null, true),
            createMessage("Non returned messaged due to count limit", null)
        );
        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        when(configuration.isHeadersInPayload()).thenReturn(withHeadersAndMetadata);
        when(configuration.isMetadataInPayload()).thenReturn(withHeadersAndMetadata);

        cut.handleResponse(ctx).test().assertComplete();

        assertThat(responseHttpHeaders).isNotNull();
        assertThat(responseHttpHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_XML);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

        chunkObs
            .await()
            .assertComplete()
            .assertValueCount(6)
            .assertValueAt(0, message -> message.toString().equals("<response><items>"))
            .assertValueAt(
                1,
                message -> {
                    if (withHeadersAndMetadata) {
                        assertThat(message.toString())
                            .startsWith(
                                "<item><id>1</id><content><![CDATA[<foo>1</foo>]]></content><headers><X-My-Header-1>headerValue1</X-My-Header-1></headers><metadata><sourceTimestamp>"
                            )
                            .endsWith("</sourceTimestamp><myKey>myValue1</myKey></metadata></item>");
                    } else {
                        assertThat(message).hasToString("<item><id>1</id><content><![CDATA[<foo>1</foo>]]></content></item>");
                    }
                    return true;
                }
            )
            .assertValueAt(2, message -> message.toString().equals("</items>"))
            .assertValueAt(
                3,
                message -> {
                    if (withHeadersAndMetadata) {
                        assertThat(message.toString())
                            .startsWith(
                                "<error><id>2</id><content><![CDATA[2]]></content><headers><X-My-Header-2>headerValue2</X-My-Header-2></headers><metadata><sourceTimestamp>"
                            )
                            .endsWith("</sourceTimestamp><myKey>myValue2</myKey></metadata></error>");
                    } else {
                        assertThat(message).hasToString("<error><id>2</id><content><![CDATA[2]]></content></error>");
                    }
                    return true;
                }
            )
            .assertValueAt(4, message -> message.toString().equals("<pagination><nextCursor>2</nextCursor><limit>2</limit></pagination>"))
            .assertValueAt(5, message -> message.toString().equals("</response>"));

        verify(ctx).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID, "1");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldReturnTextPlainArrayOfMessageWithErrorWhenResponseMessagesContainErrorMessage(boolean withHeadersAndMetadata)
        throws InterruptedException {
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE)).thenReturn(MediaType.TEXT_PLAIN);
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID)).thenReturn("2");
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(5_000L);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(2);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(HttpGetEntrypointConnector.CURSOR_QUERY_PARAM, "0");
        queryParams.add(HttpGetEntrypointConnector.LIMIT_QUERY_PARAM, "2");
        when(request.parameters()).thenReturn(queryParams);
        when(ctx.request()).thenReturn(request);

        final HttpHeaders responseHttpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHttpHeaders);
        Flowable<Message> messages = Flowable.just(
            createMessage("1", null),
            createMessage("2", null, true),
            createMessage("Non returned messaged due to count limit", null)
        );
        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        when(configuration.isHeadersInPayload()).thenReturn(withHeadersAndMetadata);
        when(configuration.isMetadataInPayload()).thenReturn(withHeadersAndMetadata);

        when(configuration.isHeadersInPayload()).thenReturn(withHeadersAndMetadata);
        when(configuration.isMetadataInPayload()).thenReturn(withHeadersAndMetadata);

        cut.handleResponse(ctx).test().assertComplete();

        assertThat(responseHttpHeaders).isNotNull();
        assertThat(responseHttpHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

        chunkObs
            .await()
            .assertComplete()
            .assertValueCount(4)
            .assertValueAt(0, message -> message.toString().equals("items\n"))
            .assertValueAt(
                1,
                message -> {
                    if (withHeadersAndMetadata) {
                        final Map<String, String> messageMap = messageToMap(message);
                        assertThat(messageMap)
                            .containsEntry("id", "1")
                            .containsEntry("content", "1")
                            .containsEntry("headers", "{X-My-Header-1=[headerValue1]}")
                            .hasEntrySatisfying(
                                "metadata",
                                value -> {
                                    assertThat(value).startsWith("{sourceTimestamp").endsWith("myKey=myValue1}");
                                }
                            );
                    } else {
                        assertThat(message).hasToString("item\n" + "id: 1\n" + "content: 1\n");
                    }
                    return true;
                }
            )
            .assertValueAt(
                2,
                message -> {
                    if (withHeadersAndMetadata) {
                        final Map<String, String> messageMap = messageToMap(message);
                        assertThat(messageMap)
                            .containsEntry("id", "2")
                            .containsEntry("content", "2")
                            .containsEntry("headers", "{X-My-Header-2=[headerValue2]}")
                            .hasEntrySatisfying(
                                "metadata",
                                value -> {
                                    assertThat(value).startsWith("{sourceTimestamp").endsWith("myKey=myValue2}");
                                }
                            );
                    } else {
                        assertThat(message).hasToString("\nerror\n" + "id: 2\n" + "content: 2\n");
                    }
                    return true;
                }
            )
            .assertValueAt(3, message -> message.toString().equals("\npagination\nnextCursor: 2\nlimit: 2"));

        verify(ctx).putInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_LAST_MESSAGE_ID, "1");
    }

    @Test
    void shouldCompleteWithStopMessageWhenStopping() throws Exception {
        final TestScheduler testScheduler = new TestScheduler();
        when(ctx.getInternalAttribute(HttpGetEntrypointConnector.ATTR_INTERNAL_RESPONSE_CONTENT_TYPE))
            .thenReturn(MediaType.APPLICATION_JSON);
        when(response.headers()).thenReturn(HttpHeaders.create());
        when(response.messages())
            .thenReturn(
                Flowable
                    .<Message>just(new DefaultMessage("test"), new DefaultMessage("test"), new DefaultMessage("test"))
                    .zipWith(Flowable.interval(1000, TimeUnit.MILLISECONDS, testScheduler), (message, aLong) -> message)
            );
        when(ctx.response()).thenReturn(response);

        cut.handleResponse(ctx).test().assertComplete();

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

        chunkObs.assertNotComplete();
        testScheduler.triggerActions();
        chunkObs.assertValueAt(0, buffer -> buffer.toString().equals("{\"items\":["));

        // Should have a message after 1s.
        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();
        chunkObs.assertValueAt(1, buffer -> buffer.toString().contains("test"));

        // Should have another message after 1s.
        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();
        chunkObs.assertValueAt(2, buffer -> buffer.toString().contains("test"));

        // Trigger stop.
        cut.doStop();

        // Should have completed.
        chunkObs.assertComplete();

        // Message items should be closed.
        chunkObs.assertValueAt(3, buffer -> buffer.toString().equals("]"));

        // Next buffer should be an error indicating the stop is in progress.
        chunkObs.assertValueAt(4, buffer -> buffer.toString().equals(",\"error\":"));
        chunkObs.assertValueAt(
            5,
            buffer -> {
                assertThat(buffer.toString())
                    .startsWith(
                        "{\"id\":\"goaway\",\"content\":\"Stopping, please reconnect\",\"headers\":{},\"metadata\":{\"sourceTimestamp\":"
                    )
                    .endsWith("}}");
                return true;
            }
        );
    }

    private Message createMessage(String messageContent, String contentType) {
        return createMessage(messageContent, contentType, false);
    }

    private Message createMessage(String messageContent, String contentType, final boolean onError) {
        String content = messageContent;
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            content = "{\"foo\": \"" + messageContent + "\"}";
        } else if (MediaType.APPLICATION_XML.equals(contentType)) {
            content = "<foo>" + messageContent + "</foo>";
        }

        final HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("myKey", "myValue" + messageContent);

        return DefaultMessage
            .builder()
            .error(onError)
            .id(messageContent)
            .headers(HttpHeaders.create().set("X-My-Header-" + messageContent, "headerValue" + messageContent))
            .content(Buffer.buffer(content))
            .metadata(metadata)
            .build();
    }
}
