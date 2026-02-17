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
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
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
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.helpers.NOPLogger;

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
        lenient().when(ctx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);

        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        lenient().when(templateEngine.getTemplateContext()).thenReturn(templateContext);
        lenient().when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);
        lenient().when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE)).thenReturn(evaluableResponse);
    }

    @Nested
    class Content {

        @Test
        void should_provide_deferred_request_content_variables() {
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT), any(Completable.class));
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_JSON), any(Completable.class));
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), any(Completable.class));
        }

        @Test
        void should_provide_deferred_response_content_variables() {
            cut.provide(ctx);

            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT), any(Completable.class));
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_JSON), any(Completable.class));
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_XML), any(Completable.class));
        }

        @Test
        void should_provide_request_content_payload() {
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
        void should_provide_response_content_payload() {
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
    }

    @Nested
    class JsonContent {

        @Test
        void should_provide_request_json_content_payload() {
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
        void should_provide_response_json_content_payload() {
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
    }

    @Nested
    class XmlContent {

        @Test
        void should_provide_request_xml_content_payload() {
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
        void should_provide_response_xml_content_payload() {
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
        void should_provide_xml_content_when_empty_content() {
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
        void should_provide_xml_content_when_invalid_xml_content() {
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
        void should_provide_request_xml_content_with_mixed_content() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <root>
                           value
                           <key>1</key>
                        </root>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(evaluableRequest).setXmlContent(captor.capture());
            assertThat(captor.getValue()).containsExactlyInAnyOrderEntriesOf(
                ofEntries(entry("root", ofEntries(entry("", "\n   value\n   "), entry("key", "1"))))
            );
        }

        @Test
        void should_provide_request_xml_content_with_duplicated_keys() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <root>
                           <key>1</key>
                           <key>2</key>
                        </root>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(ofEntries(entry("root", Map.of("key", List.of("1", "2")))));
        }

        @Test
        void should_provide_request_xml_content_with_xml_declaration() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <?xml version="1.0" ?>
                        <root>
                           <key>1</key>
                        </root>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(ofEntries(entry("root", Map.of("key", "1"))));
        }

        @Test
        void should_provide_request_xml_content_with_tag_having_attributes() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <users xmlns="http://example.com" version="1.0" from="api">
                           <user>John</user>
                        </users>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(
                ofEntries(entry("users", ofEntries(entry("version", "1.0"), entry("from", "api"), entry("user", "John"))))
            );
        }

        @Test
        void should_provide_request_xml_content_with_wrap_tag() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <wrap>
                            <user>1</user>
                        </wrap>
                        <root>ok</root>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(ofEntries(entry("wrap", ofEntries(entry("user", "1"))), entry("root", "ok")));
        }

        @Test
        void should_provide_request_xml_content_with_multiple_key() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <root><key>1</key><key>2</key></root>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(ofEntries(entry("root", ofEntries(entry("key", List.of("1", "2"))))));
        }

        @Test
        void should_provide_request_xml_content_with_tag_having_namespaces() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <foo:users xmlns:foo="http://example.com" version="1.0" from="api">
                           <user>John</user>
                        </foo:users>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(
                ofEntries(entry("users", ofEntries(entry("version", "1.0"), entry("from", "api"), entry("user", "John"))))
            );
        }

        @Test
        void should_provide_request_xml_content_with_doctype() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <!DOCTYPE topic PUBLIC "-//OASIS//DTD DITA Topic//EN" "http://docs.oasis-open.org/dita/v1.1/OS/dtd/topic.dtd">
                        <users xmlns="http://example.com" version="1.0" from="api">
                           <user>John</user>
                        </users>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(
                ofEntries(entry("users", ofEntries(entry("version", "1.0"), entry("from", "api"), entry("user", "John"))))
            );
        }

        @Test
        void should_provide_request_xml_content_with_tag_having_namespaces_and_prologue() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <?xml version="1.0"?>
                        <foo:users xmlns:foo="http://example.com" version="1.0" from="api">
                           <user>John</user>
                        </foo:users>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(
                ofEntries(entry("users", ofEntries(entry("version", "1.0"), entry("from", "api"), entry("user", "John"))))
            );
        }

        @Test
        void should_provide_request_xml_content_with_doctype_and_prologue() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <?xml version="1.0"?>
                        <!DOCTYPE topic PUBLIC "-//OASIS//DTD DITA Topic//EN" "http://docs.oasis-open.org/dita/v1.1/OS/dtd/topic.dtd">
                        <users xmlns="http://example.com" version="1.0" from="api">
                           <user>John</user>
                        </users>
                        """
                    )
                )
            );
            when(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)).thenReturn(evaluableRequest);

            cut.provide(ctx);

            ArgumentCaptor<Completable> completableCaptor = ArgumentCaptor.forClass(Completable.class);
            verify(templateContext).setDeferredVariable(eq(TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML), completableCaptor.capture());

            final Completable contentCompletable = completableCaptor.getValue();
            final TestObserver<Void> obs = contentCompletable.test();
            obs.assertComplete();

            verify(evaluableRequest).setXmlContent(
                ofEntries(entry("users", ofEntries(entry("version", "1.0"), entry("from", "api"), entry("user", "John"))))
            );
        }

        @Test
        void should_provide_empty_map_when_prolog_and_doctype_only() {
            when(request.bodyOrEmpty()).thenReturn(
                Single.just(
                    Buffer.buffer(
                        """
                        <?xml version="1.0"?>
                        <!DOCTYPE topic PUBLIC "-//OASIS//DTD DITA Topic//EN" "http://docs.oasis-open.org/dita/v1.1/OS/dtd/topic.dtd">
                        """
                    )
                )
            );
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
    }
}
