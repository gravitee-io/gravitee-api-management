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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error;

import static io.gravitee.gateway.api.http.HttpHeaderNames.ACCEPT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.handlers.api.processor.error.ExecutionFailureAsJson;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.AbstractV4ProcessorTest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SimpleFailureMessageProcessorTest extends AbstractV4ProcessorTest {

    private static final ObjectMapper mapper = new GraviteeMapper();

    private final MemoryAppender memoryAppender = new MemoryAppender();

    @Captor
    ArgumentCaptor<Buffer> bufferCaptor;

    private SimpleFailureMessageProcessor simpleFailureMessageProcessor;

    @BeforeEach
    public void beforeEach() {
        simpleFailureMessageProcessor = SimpleFailureMessageProcessor.instance();
    }

    @Test
    void should_catch_and_ignore_error_message_with_interruption_execution() throws InterruptedException {
        simpleFailureMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable.just(new DefaultMessage("1")).flatMap(defaultMessage -> ctx.interruptMessages()).compose(requestMessages).test().await();

        ArgumentCaptor<FlowableTransformer<Message, Message>> responseMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockResponse, times(1)).onMessages(responseMessagesCaptor.capture());

        FlowableTransformer<Message, Message> responseMessages = responseMessagesCaptor.getValue();
        TestSubscriber<Message> testSubscriber = Flowable.<Message>empty().compose(responseMessages).test();
        testSubscriber.await();
        testSubscriber.assertValueCount(0);
    }

    @Test
    void should_catch_and_send_error_message_with_execution_failure() throws InterruptedException {
        simpleFailureMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage ->
                ctx.interruptMessagesWith(
                    new ExecutionFailure(HttpResponseStatus.NOT_FOUND.code()).message(HttpResponseStatus.NOT_FOUND.reasonPhrase())
                )
            )
            .compose(requestMessages)
            .test()
            .await();

        ArgumentCaptor<FlowableTransformer<Message, Message>> responseMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockResponse, times(1)).onMessages(responseMessagesCaptor.capture());
        FlowableTransformer<Message, Message> responseMessages = responseMessagesCaptor.getValue();
        TestSubscriber<Message> testSubscriber = Flowable.just(new DefaultMessage("2")).compose(responseMessages).test();
        testSubscriber.await();
        testSubscriber
            .assertValueCount(1)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.id()).isNotNull();
                    assertThat(message.error()).isTrue();
                    assertThat(message.content()).hasToString(HttpResponseStatus.NOT_FOUND.reasonPhrase());
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
                    assertThat(message.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN);
                    return true;
                }
            );
    }

    @Test
    void should_catch_and_send_error_message_without_execution_failure() throws InterruptedException {
        simpleFailureMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage -> Flowable.<Message>error(new RuntimeException("error")))
            .compose(requestMessages)
            .test()
            .await();

        ArgumentCaptor<FlowableTransformer<Message, Message>> responseMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockResponse, times(1)).onMessages(responseMessagesCaptor.capture());
        FlowableTransformer<Message, Message> responseMessages = responseMessagesCaptor.getValue();
        TestSubscriber<Message> testSubscriber = Flowable.just(new DefaultMessage("2")).compose(responseMessages).test();
        testSubscriber.await();
        testSubscriber
            .assertValueCount(1)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.id()).isNotNull();
                    assertThat(message.error()).isTrue();
                    assertThat(message.content()).hasToString(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
                    assertThat(message.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN);
                    return true;
                }
            );
    }

    @Test
    void should_catch_and_send_json_error_message_with_accept_header_json() throws JsonProcessingException, InterruptedException {
        spyRequestHeaders.add(ACCEPT, List.of(MediaType.APPLICATION_JSON));
        simpleFailureMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.NOT_FOUND.code())
            .message(HttpResponseStatus.NOT_FOUND.reasonPhrase());
        String contentAsJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage -> ctx.interruptMessagesWith(executionFailure))
            .compose(requestMessages)
            .test()
            .await();

        ArgumentCaptor<FlowableTransformer<Message, Message>> responseMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockResponse, times(1)).onMessages(responseMessagesCaptor.capture());
        FlowableTransformer<Message, Message> responseMessages = responseMessagesCaptor.getValue();
        TestSubscriber<Message> testSubscriber = Flowable.just(new DefaultMessage("2")).compose(responseMessages).test();
        testSubscriber.await();
        testSubscriber
            .assertValueCount(1)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.id()).isNotNull();
                    assertThat(message.error()).isTrue();
                    assertThat(message.content()).hasToString(contentAsJson);
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
                    assertThat(message.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);

                    return true;
                }
            );
    }

    @Test
    void should_catch_and_send_json_error_message_with_accept_wildcard() throws JsonProcessingException, InterruptedException {
        spyRequestHeaders.add(ACCEPT, List.of(MediaType.WILDCARD));
        simpleFailureMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.NOT_FOUND.code())
            .message(HttpResponseStatus.NOT_FOUND.reasonPhrase());
        String contentAsJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage -> ctx.interruptMessagesWith(executionFailure))
            .compose(requestMessages)
            .test()
            .await();

        ArgumentCaptor<FlowableTransformer<Message, Message>> responseMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockResponse, times(1)).onMessages(responseMessagesCaptor.capture());
        FlowableTransformer<Message, Message> responseMessages = responseMessagesCaptor.getValue();
        TestSubscriber<Message> testSubscriber = Flowable.just(new DefaultMessage("2")).compose(responseMessages).test();
        testSubscriber.await();
        testSubscriber
            .assertValueCount(1)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.id()).isNotNull();
                    assertThat(message.error()).isTrue();
                    assertThat(message.content()).hasToString(contentAsJson);
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
                    assertThat(message.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);

                    return true;
                }
            );
    }

    @Test
    void should_catch_and_send_json_error_message_with_accept_json_and_content_type_json() throws InterruptedException {
        spyRequestHeaders.add(ACCEPT, List.of(MediaType.APPLICATION_JSON));
        simpleFailureMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        String contentAsJson = "{\"text\": \"error\"}";
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.NOT_FOUND.code())
            .message(contentAsJson)
            .contentType(MediaType.APPLICATION_JSON);
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage -> ctx.interruptMessagesWith(executionFailure))
            .compose(requestMessages)
            .test()
            .await();

        ArgumentCaptor<FlowableTransformer<Message, Message>> responseMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockResponse, times(1)).onMessages(responseMessagesCaptor.capture());
        FlowableTransformer<Message, Message> responseMessages = responseMessagesCaptor.getValue();
        TestSubscriber<Message> testSubscriber = Flowable.just(new DefaultMessage("2")).compose(responseMessages).test();
        testSubscriber.await();
        testSubscriber
            .assertValueCount(1)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.id()).isNotNull();
                    assertThat(message.error()).isTrue();
                    assertThat(message.content()).hasToString(contentAsJson);
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
                    assertThat(message.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);

                    return true;
                }
            );
    }

    @Test
    void should_complete_with_txt_error_and_no_accept_header() throws InterruptedException {
        simpleFailureMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        String contentAsText = "error";
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.NOT_FOUND.code()).message(contentAsText);
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage -> ctx.interruptMessagesWith(executionFailure))
            .compose(requestMessages)
            .test()
            .await();

        ArgumentCaptor<FlowableTransformer<Message, Message>> responseMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockResponse, times(1)).onMessages(responseMessagesCaptor.capture());
        FlowableTransformer<Message, Message> responseMessages = responseMessagesCaptor.getValue();
        TestSubscriber<Message> testSubscriber = Flowable.just(new DefaultMessage("2")).compose(responseMessages).test();
        testSubscriber.await();
        testSubscriber
            .assertValueCount(1)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.id()).isNotNull();
                    assertThat(message.error()).isTrue();
                    assertThat(message.content()).hasToString(contentAsText);
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
                    assertThat(message.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN);

                    return true;
                }
            );
    }

    @Test
    void should_log_message_when_failure_has_exception_parameter() throws InterruptedException {
        configureMemoryAppender();
        when(mockMetrics.getApi()).thenReturn("api-id");

        simpleFailureMessageProcessor.execute(ctx).test().assertResult();

        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage ->
                ctx.interruptMessagesWith(
                    new ExecutionFailure(HttpResponseStatus.NOT_FOUND.code())
                        .message(HttpResponseStatus.NOT_FOUND.reasonPhrase())
                        .parameters(Map.of("exception", new RuntimeException("root exception")))
                )
            )
            .compose(requestMessages)
            .test()
            .await();

        ArgumentCaptor<FlowableTransformer<Message, Message>> responseMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockResponse, times(1)).onMessages(responseMessagesCaptor.capture());
        FlowableTransformer<Message, Message> responseMessages = responseMessagesCaptor.getValue();
        TestSubscriber<Message> testSubscriber = Flowable.just(new DefaultMessage("2")).compose(responseMessages).test();
        testSubscriber.await();
        testSubscriber.assertValueCount(1);

        Assertions.assertThat(memoryAppender.getLoggedEvents()).hasSize(1);
        SoftAssertions.assertSoftly(soft -> {
            var event = memoryAppender.getLoggedEvents().get(0);
            soft.assertThat(event.getMessage()).contains("An error occurred while processing message");
            soft.assertThat(event.getArgumentArray()).contains("api-id");
            soft.assertThat(event.getThrowableProxy().getMessage()).isEqualTo("root exception");
        });
    }

    private void configureMemoryAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractFailureMessageProcessor.class);
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }
}
