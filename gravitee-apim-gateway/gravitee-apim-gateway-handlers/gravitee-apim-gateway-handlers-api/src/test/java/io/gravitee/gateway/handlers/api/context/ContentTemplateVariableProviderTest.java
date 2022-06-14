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
package io.gravitee.gateway.handlers.api.context;

import static io.gravitee.gateway.handlers.api.context.ContentTemplateVariableProvider.*;
import static io.gravitee.gateway.jupiter.api.context.RequestExecutionContext.TEMPLATE_ATTRIBUTE_REQUEST;
import static io.gravitee.gateway.jupiter.api.context.RequestExecutionContext.TEMPLATE_ATTRIBUTE_RESPONSE;
import static org.mockito.Mockito.*;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.el.EvaluableRequest;
import io.gravitee.gateway.jupiter.api.el.EvaluableResponse;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.util.Collections;
import java.util.Map;
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

    @Mock
    private RequestExecutionContext ctx;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private TemplateContext templateContext;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private EvaluableRequest evaluableRequest;

    @Mock
    private EvaluableResponse evaluableResponse;

    private final ContentTemplateVariableProvider cut = new ContentTemplateVariableProvider();

    @BeforeEach
    void init() {
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);

        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getTemplateContext()).thenReturn(templateContext);
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE)).thenReturn(evaluableResponse);
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
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("invalid")));
        when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

        cut.provide(ctx);

        ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
        verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

        final Completable contentCompletable = completableCaptor.getValue();
        final TestObserver<Void> obs = contentCompletable.test();
        obs.assertComplete();

        verify(evaluableRequest).setXmlContent(Collections.emptyMap());
    }
}
