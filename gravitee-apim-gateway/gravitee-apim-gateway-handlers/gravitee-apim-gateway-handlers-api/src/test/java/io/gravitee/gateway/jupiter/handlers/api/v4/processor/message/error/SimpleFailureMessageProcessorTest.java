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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SimpleFailureMessageProcessorTest extends AbstractV4ProcessorTest {

    private static final ObjectMapper mapper = new GraviteeMapper();

    @Captor
    ArgumentCaptor<Buffer> bufferCaptor;

    private SimpleFailureMessageProcessor simpleFailureMessageProcessor;

    @BeforeEach
    public void beforeEach() {
        simpleFailureMessageProcessor = SimpleFailureMessageProcessor.instance();
    }

    @Test
    void shouldCatchAndIgnoreErrorMessageWithInterruptionExecution() throws InterruptedException {
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
    void shouldCatchAndSendErrorMessageWithExecutionFailure() throws InterruptedException {
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
    void shouldCatchAndSendErrorMessageWithoutExecutionFailure() throws InterruptedException {
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
    void shouldCatchAndSendJsonErrorMessageWithAcceptHeaderJson() throws JsonProcessingException, InterruptedException {
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
    void shouldCatchAndSendJsonErrorMessageWithAcceptWildcard() throws JsonProcessingException, InterruptedException {
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
    void shouldCatchAndSendJsonErrorMessageWithAcceptJsonAndContentTypeJson() throws InterruptedException {
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
    void shouldCompleteWithTxtErrorAndNoAcceptHeader() throws InterruptedException {
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
}
