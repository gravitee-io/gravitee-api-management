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
import static io.reactivex.rxjava3.core.Single.defer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.HttpRequest;
import io.gravitee.gateway.reactive.api.context.HttpResponse;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.el.EvaluableRequest;
import io.gravitee.gateway.reactive.api.el.EvaluableResponse;
import io.gravitee.gateway.reactive.core.context.ExecutionContextTemplateVariableProvider;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.CustomLog;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * {@link TemplateVariableProvider} allowing to add access to request and response content from the template engine.
 * It basically adds deferred variables to the template context that will be dynamically resolved on demand.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ContentTemplateVariableProvider implements ExecutionContextTemplateVariableProvider {

    protected static final String TEMPLATE_ATTRIBUTE_REQUEST_CONTENT = TEMPLATE_ATTRIBUTE_REQUEST + ".content";
    protected static final String TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_JSON = TEMPLATE_ATTRIBUTE_REQUEST + ".jsonContent";
    protected static final String TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML = TEMPLATE_ATTRIBUTE_REQUEST + ".xmlContent";
    protected static final String TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT = TEMPLATE_ATTRIBUTE_RESPONSE + ".content";
    protected static final String TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_JSON = TEMPLATE_ATTRIBUTE_RESPONSE + ".jsonContent";
    protected static final String TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_XML = TEMPLATE_ATTRIBUTE_RESPONSE + ".xmlContent";

    private static final TypeReference<HashMap<String, Object>> MAPPER_TYPE_REFERENCE = new TypeReference<>() {};
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newFactory();
    private static final XmlMapper XML_MAPPER = new XmlMapper(XML_INPUT_FACTORY);

    @Override
    public <T extends BaseExecutionContext> void provide(T executionContext) {
        HttpExecutionContext ctx = (HttpExecutionContext) executionContext;
        final TemplateEngine templateEngine = ctx.getTemplateEngine();
        final TemplateContext templateContext = templateEngine.getTemplateContext();
        final EvaluableRequest evaluableRequest = (EvaluableRequest) templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST);
        final EvaluableResponse evaluableResponse = (EvaluableResponse) templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE);
        final HttpRequest request = ctx.request();
        final HttpResponse response = ctx.response();

        if (evaluableRequest != null) {
            var bodyRequestDefer = defer(request::bodyOrEmpty);

            templateContext.setDeferredVariable(
                TEMPLATE_ATTRIBUTE_REQUEST_CONTENT,
                bodyRequestDefer.map(Buffer::toString).doOnSuccess(evaluableRequest::setContent).ignoreElement()
            );

            templateContext.setDeferredVariable(
                TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_JSON,
                bodyRequestDefer.map(this::jsonToMap).doOnSuccess(evaluableRequest::setJsonContent).ignoreElement()
            );

            templateContext.setDeferredVariable(
                TEMPLATE_ATTRIBUTE_REQUEST_CONTENT_XML,
                bodyRequestDefer
                    .map(buffer -> xmlToMap(ctx, buffer))
                    .doOnSuccess(evaluableRequest::setXmlContent)
                    .ignoreElement()
            );
        }

        if (evaluableResponse != null) {
            var bodyResponseDefer = defer(response::bodyOrEmpty);

            templateContext.setDeferredVariable(
                TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT,
                bodyResponseDefer.map(Buffer::toString).doOnSuccess(evaluableResponse::setContent).ignoreElement()
            );
            templateContext.setDeferredVariable(
                TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_JSON,
                bodyResponseDefer.map(this::jsonToMap).doOnSuccess(evaluableResponse::setJsonContent).ignoreElement()
            );

            templateContext.setDeferredVariable(
                TEMPLATE_ATTRIBUTE_RESPONSE_CONTENT_XML,
                bodyResponseDefer
                    .map(buffer -> xmlToMap(ctx, buffer))
                    .doOnSuccess(evaluableResponse::setXmlContent)
                    .ignoreElement()
            );
        }
    }

    private Map<String, Object> jsonToMap(Buffer buffer) {
        try {
            if (buffer.length() == 0) {
                return Collections.emptyMap();
            }

            return JSON_MAPPER.readValue(buffer.toString(), MAPPER_TYPE_REFERENCE);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> xmlToMap(HttpExecutionContext executionContext, Buffer buffer) {
        try {
            if (buffer.length() == 0) {
                return Collections.emptyMap();
            }
            String sanitizedContent = sanitizeContent(executionContext, buffer);
            if (sanitizedContent.isEmpty()) {
                return Collections.emptyMap();
            }
            String wrappedContent = wrapContent(sanitizedContent);
            return XML_MAPPER.readValue(wrappedContent, MAPPER_TYPE_REFERENCE);
        } catch (XMLStreamException | JsonProcessingException e) {
            executionContext.warnWith(new ExecutionWarn("INVALID_XML").message("XML content is invalid").cause(e));
            return Collections.emptyMap();
        }
    }

    /**
     * Remove XML prolog and DocType tags
     *
     * @param ctx
     * @param buffer           the XML content to sanitized
     * @return the sanitized XML
     */
    private String sanitizeContent(HttpExecutionContext ctx, Buffer buffer) throws XMLStreamException {
        XMLStreamReader sr = XML_INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(buffer.getBytes()));
        while (!sr.isStartElement() && sr.hasNext()) {
            try {
                sr.next();
            } catch (Exception e) {
                ctx.withLogger(log).debug("Ignoring error while parsing XML content to find a start element", e);
            }
        }
        if (sr.isStartElement()) {
            return buffer.toString().substring(sr.getLocation().getCharacterOffset());
        }
        return "";
    }

    /**
     * Wrap xml content into a randomly generated xml tag.
     * This generated tag allow to handle multiple root tag in the initial content.
     *
     * @param content the XML content to wrap
     * @return the wrapped XML
     */
    private String wrapContent(String content) {
        String generatedString = RandomStringUtils.insecure().next(10, true, false);

        return "<" + generatedString + ">" + content + "</" + generatedString + ">";
    }
}
