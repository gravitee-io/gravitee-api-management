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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.template;

import static io.gravitee.gateway.api.http.HttpHeaderNames.ACCEPT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.AbstractV4ProcessorTest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class ResponseTemplateBasedFailureMessageProcessorTest extends AbstractV4ProcessorTest {

    private ResponseTemplateBasedFailureMessageProcessor responseTemplateBasedFailureMessageProcessor;

    @BeforeEach
    public void beforeEach() {
        responseTemplateBasedFailureMessageProcessor = ResponseTemplateBasedFailureMessageProcessor.instance();
    }

    @Test
    void shouldFallbackToDefaultWithNoProcessorFailureKey() throws InterruptedException {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureMessageProcessor.WILDCARD_CONTENT_TYPE, template);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);
        api.setResponseTemplates(templates);

        // Set failure
        responseTemplateBasedFailureMessageProcessor.execute(ctx).test().assertResult();
        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage -> ctx.interruptMessagesWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)))
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
                    assertThat(message.content()).isNull();
                    assertThat(message.headers()).isNotNull();
                    assertThat(message.metadata().get("statusCode")).isEqualTo(500);
                    return true;
                }
            );
    }

    @Test
    void shouldFallbackToDefaultWithProcessorFailureKey() throws InterruptedException {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureMessageProcessor.WILDCARD_CONTENT_TYPE, template);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);
        api.setResponseTemplates(templates);

        responseTemplateBasedFailureMessageProcessor.execute(ctx).test().assertResult();
        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage ->
                ctx.interruptMessagesWith(new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400).key("POLICY_ERROR_KEY"))
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
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isFalse();
                    assertThat(message.metadata().get("key")).isEqualTo("POLICY_ERROR_KEY");
                    assertThat(message.metadata().get("statusCode")).isEqualTo(400);
                    return true;
                }
            );
    }

    @Test
    void shouldFallbackToDefaultWithProcessorFailureKeyAndFallbackToDefault() throws InterruptedException {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureMessageProcessor.WILDCARD_CONTENT_TYPE, template);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("DEFAULT", mapTemplates);
        api.setResponseTemplates(templates);

        responseTemplateBasedFailureMessageProcessor.execute(ctx).test().assertResult();
        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage ->
                ctx.interruptMessagesWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500).key("POLICY_ERROR_KEY"))
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
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
                    assertThat(message.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isFalse();
                    assertThat(message.metadata().get("key")).isEqualTo("POLICY_ERROR_KEY");
                    assertThat(message.metadata().get("statusCode")).isEqualTo(400);
                    return true;
                }
            );
    }

    @Test
    void shouldFallbackToDefaultHandlerWithProcessorFailureKeyAndUnmappedAcceptHeader() throws InterruptedException {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, template);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);
        api.setResponseTemplates(templates);

        responseTemplateBasedFailureMessageProcessor.execute(ctx).test().assertResult();
        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage ->
                ctx.interruptMessagesWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500).key("POLICY_ERROR_KEY"))
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
                    assertThat(message.headers()).isNotNull();
                    assertThat(message.metadata().get("key")).isEqualTo("POLICY_ERROR_KEY");
                    assertThat(message.metadata().get("statusCode")).isEqualTo(500);
                    return true;
                }
            );
    }

    @Test
    void shouldFallbackToDefaultHandlerWithProcessorFailureKeyAndUnmappedAcceptHeaderAndWildcard() throws InterruptedException {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        ResponseTemplate wildcardTemplate = new ResponseTemplate();
        wildcardTemplate.setStatusCode(HttpStatusCode.BAD_GATEWAY_502);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, template);
        mapTemplates.put(ResponseTemplateBasedFailureMessageProcessor.WILDCARD_CONTENT_TYPE, wildcardTemplate);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);
        api.setResponseTemplates(templates);

        // Set failure
        spyRequestHeaders.add(ACCEPT, Collections.singletonList(MediaType.APPLICATION_XML));

        responseTemplateBasedFailureMessageProcessor.execute(ctx).test().assertResult();
        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage ->
                ctx.interruptMessagesWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500).key("POLICY_ERROR_KEY"))
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
                    assertThat(message.headers()).isNotNull();
                    assertThat(message.metadata().get("key")).isEqualTo("POLICY_ERROR_KEY");
                    assertThat(message.metadata().get("statusCode")).isEqualTo(502);
                    return true;
                }
            );
    }

    @Test
    void shouldFallbackToDefaultHandlerWithProcessorFailureKeyAndAcceptHeader() throws InterruptedException {
        ResponseTemplate jsonTemplate = new ResponseTemplate();
        jsonTemplate.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, jsonTemplate);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);
        api.setResponseTemplates(templates);
        spyRequestHeaders.add(ACCEPT, Collections.singletonList(MediaType.APPLICATION_JSON));
        responseTemplateBasedFailureMessageProcessor.execute(ctx).test().assertResult();
        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage ->
                ctx.interruptMessagesWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500).key("POLICY_ERROR_KEY"))
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
                    assertThat(message.headers()).isNotNull();
                    assertThat(message.metadata().get("key")).isEqualTo("POLICY_ERROR_KEY");
                    assertThat(message.metadata().get("statusCode")).isEqualTo(400);
                    return true;
                }
            );
    }

    @Test
    void shouldFallbackToDefaultHandlerWithProcessorFailureKeyAndAcceptHeaderAndNextMediaType() throws InterruptedException {
        ResponseTemplate jsonTemplate = new ResponseTemplate();
        jsonTemplate.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, jsonTemplate);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);
        api.setResponseTemplates(templates);

        spyRequestHeaders.add(ACCEPT, List.of("text/html", " application/json", "*/*;q=0.8", "application/xml;q=0.9"));
        responseTemplateBasedFailureMessageProcessor.execute(ctx).test().assertResult();
        ArgumentCaptor<FlowableTransformer<Message, Message>> requestMessagesCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        verify(mockRequest).onMessages(requestMessagesCaptor.capture());

        FlowableTransformer<Message, Message> requestMessages = requestMessagesCaptor.getValue();
        Flowable
            .just(new DefaultMessage("1"))
            .flatMap(defaultMessage ->
                ctx.interruptMessagesWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500).key("POLICY_ERROR_KEY"))
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
                    assertThat(message.headers()).isNotNull();
                    assertThat(message.metadata().get("key")).isEqualTo("POLICY_ERROR_KEY");
                    assertThat(message.metadata().get("statusCode")).isEqualTo(400);
                    return true;
                }
            );
    }
}
