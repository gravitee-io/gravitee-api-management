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
package io.gravitee.gateway.reactive.handlers.api.el;

import static io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext.TEMPLATE_ATTRIBUTE_REQUEST;
import static io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext.TEMPLATE_ATTRIBUTE_RESPONSE;
import static io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider.TEMPLATE_ATTRIBUTE_REQUEST_CONTENT;
import static io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider.TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_JSON;
import static io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider.TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML;
import static io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider.TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT;
import static io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider.TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_JSON;
import static io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider.TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_XML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.HttpRequest;
import io.gravitee.gateway.reactive.api.context.HttpResponse;
import io.gravitee.gateway.reactive.api.el.EvaluableRequest;
import io.gravitee.gateway.reactive.api.el.EvaluableResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ContentTemplateVariableProviderTest {

    protected static final String REQUEST_CONTENT = "request content";
    protected static final String REQUEST_JSON_CONTENT = "{\"request\": \"content\"}";
    protected static final String REQUEST_XML_CONTENT = "<request>content</request>";

    protected static final String RESPONSE_CONTENT = "response content";
    protected static final String RESPONSE_JSON_CONTENT = "{\"response\": \"content\"}";
    protected static final String RESPONSE_XML_CONTENT = "<response>content</response>";
    private final ContentTemplateVariableProvider cut = new ContentTemplateVariableProvider();

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private TemplateContext templateContext;

    @Mock
    private HttpRequest request;

    @Mock
    private HttpResponse response;

    @Mock
    private EvaluableRequest evaluableRequest;

    @Mock
    private EvaluableResponse evaluableResponse;

    @BeforeEach
    void init() {
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);

        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        lenient().when(templateEngine.getTemplateContext()).thenReturn(templateContext);
        lenient().when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);
        lenient().when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE)).thenReturn(evaluableResponse);
    }

    @Test
    void shouldProvideDeferredRequestContentVariables() {
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT), any(Completable.class));
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_JSON), any(Completable.class));
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), any(Completable.class));
    }

    @Test
    void shouldProvideDeferredResponseContentVariables() {
        cut.provide(ctx);

        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT), any(Completable.class));
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_JSON), any(Completable.class));
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_XML), any(Completable.class));
    }

    @Test
    void shouldProvideRequestContentPayload() {
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer(REQUEST_CONTENT)));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableRequest).setContent(REQUEST_CONTENT);
    }

    @Test
    void shouldProvideRequestJsonContentPayload() {
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer(REQUEST_JSON_CONTENT)));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_JSON), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableRequest).setJsonContent(Map.of("request", "content"));
    }

    @Test
    void shouldProvideRequestXmlContentPayload() {
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer(REQUEST_XML_CONTENT)));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableRequest).setXmlContent(Map.of("request", "content"));
    }

    @Test
    void shouldProvideResponseContentPayload() {
        when(response.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer(RESPONSE_CONTENT)));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE)).thenReturn(evaluableResponse);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableResponse).setContent(RESPONSE_CONTENT);
    }

    @Test
    void shouldProvideResponseJsonContentPayload() {
        when(response.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer(RESPONSE_JSON_CONTENT)));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE)).thenReturn(evaluableResponse);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_JSON), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableResponse).setJsonContent(Map.of("response", "content"));
    }

    @Test
    void shouldProvideResponseXmlContentPayload() {
        when(response.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer(RESPONSE_XML_CONTENT)));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE)).thenReturn(evaluableResponse);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_XML), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableResponse).setXmlContent(Map.of("response", "content"));
    }

    @Test
    void shouldProvideEmptyJsonContentWhenEmptyContent() {
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer()));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_JSON), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableRequest).setJsonContent(Collections.emptyMap());
    }

    @Test
    void shouldProvideEmptyJsonContentWhenInvalidJsonContent() {
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("invalid")));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_JSON), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableRequest).setJsonContent(Collections.emptyMap());
    }

    @Test
    void shouldProvideXmlContentWhenEmptyContent() {
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer()));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableRequest).setXmlContent(Collections.emptyMap());
    }

    @Test
    void shouldProvideXmlContentWhenInvalidXmlContent() {
        // Provide an invalid xml that can't be successfully parsed by the xml mapper.
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("<invalid")));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        // An empty map is expected if the content isn't a valid xml.
        verify(evaluableRequest).setXmlContent(Collections.emptyMap());
    }

    @Test
    void shouldOnlySubscribeOnceToRequestBodyOrEmpty() {
        // Use a template engine
        var engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable(TEMPLATE_ATTRIBUTE_REQUEST, evaluableRequest);
        when(ctx.getTemplateEngine()).thenReturn(engine);

        // Count the number of subscription received
        var nbOfSubscribeRequest = new AtomicInteger();
        var requestBodyObs = Single
            .just(Buffer.buffer(REQUEST_JSON_CONTENT))
            .doOnSubscribe(disposable -> nbOfSubscribeRequest.incrementAndGet());
        when(request.bodyOrEmpty()).thenReturn(requestBodyObs);

        cut.provide(ctx);

        TestObserver<Void> obsRequest = Flowable
            .fromIterable(
                List.of(
                    "{#request.content != null}",
                    "{#request.jsonContent != null}",
                    "{#request.content != null}",
                    "{#request.jsonContent != null}"
                )
            )
            .flatMapMaybe(v -> ctx.getTemplateEngine().eval(v, Object.class))
            .ignoreElements()
            .test();

        obsRequest.assertComplete();

        verify(request).bodyOrEmpty();
        verify(evaluableRequest, times(2)).setContent(any());

        // Assert that only one subscription has been done
        assertThat(nbOfSubscribeRequest).hasValue(1);
    }

    @Test
    void shouldOnlySubscribeOnceToResponseBodyOrEmpty() {
        // Use a template engine
        var engine = TemplateEngine.templateEngine();
        engine.getTemplateContext().setVariable(TEMPLATE_ATTRIBUTE_RESPONSE, evaluableResponse);
        when(ctx.getTemplateEngine()).thenReturn(engine);

        // Count the number of subscription received
        var nbOfSubscribeResponse = new AtomicInteger();
        var responseBody = Single
            .just(Buffer.buffer(RESPONSE_JSON_CONTENT))
            .doOnSubscribe(disposable -> nbOfSubscribeResponse.incrementAndGet());
        when(response.bodyOrEmpty()).thenReturn(responseBody);

        cut.provide(ctx);

        TestObserver<Void> obsResponse = Flowable
            .fromIterable(
                List.of(
                    "{#response.content != null}",
                    "{#response.jsonContent != null}",
                    "{#response.content != null}",
                    "{#response.jsonContent != null}"
                )
            )
            .flatMapMaybe(v -> ctx.getTemplateEngine().eval(v, Object.class))
            .ignoreElements()
            .test();

        obsResponse.assertComplete();

        verify(response).bodyOrEmpty();
        verify(evaluableResponse, times(2)).setJsonContent(any());

        // Assert that only one subscription has been done
        assertThat(nbOfSubscribeResponse).hasValue(1);
    }
}
