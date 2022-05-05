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
package io.gravitee.gateway.reactive.handlers.api.processor.error.template;

import static io.gravitee.gateway.api.http.HttpHeaderNames.ACCEPT;
import static io.gravitee.gateway.api.http.HttpHeaderNames.ORIGIN;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.handlers.api.processor.error.templates.DummyProcessorFailure;
import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTemplateBasedFailureProcessorTest extends AbstractProcessorTest {

    private ResponseTemplateBasedFailureProcessor processor;

    @Test
    public void shouldFallbackToDefaultWithNoProcessorFailureKey() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureProcessor.WILDCARD_CONTENT_TYPE, template);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        ctx.setAttribute(ExecutionContext.ATTR_PREFIX + "failure", failure);

        processor.execute(ctx).test().assertResult();

        verify(mockResponse, times(1)).status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void shouldFallbackToDefaultWithProcessorFailureKey() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureProcessor.WILDCARD_CONTENT_TYPE, template);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.key("POLICY_ERROR_KEY");
        failure.statusCode(HttpStatusCode.BAD_REQUEST_400);

        ctx.setAttribute(ExecutionContext.ATTR_PREFIX + "failure", failure);

        processor.execute(ctx).test().assertResult();

        verify(mockResponse, times(1)).status(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void shouldFallbackToDefaultWithProcessorFailureKeyAndFallbackToDefault() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(ResponseTemplateBasedFailureProcessor.WILDCARD_CONTENT_TYPE, template);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("DEFAULT", mapTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.key("POLICY_ERROR_KEY");
        failure.statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        ctx.setAttribute(ExecutionContext.ATTR_PREFIX + "failure", failure);

        processor.execute(ctx).test().assertResult();

        verify(mockResponse, times(1)).status(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void shouldFallbackToDefaultHandlerWithProcessorFailureKeyAndUnmappedAcceptHeader() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, template);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure();
        failure.key("POLICY_ERROR_KEY");
        failure.statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        spyRequestHeaders.add(ACCEPT, Collections.singletonList(MediaType.APPLICATION_XML));
        ctx.setAttribute(ExecutionContext.ATTR_PREFIX + "failure", failure);

        processor.execute(ctx).test().assertResult();

        verify(mockResponse, times(1)).status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void shouldFallbackToDefaultHandlerWithProcessorFailureKeyAndUnmappedAcceptHeaderAndWildcard() {
        ResponseTemplate template = new ResponseTemplate();
        template.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        ResponseTemplate wildcardTemplate = new ResponseTemplate();
        wildcardTemplate.setStatusCode(HttpStatusCode.BAD_GATEWAY_502);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, template);
        mapTemplates.put(ResponseTemplateBasedFailureProcessor.WILDCARD_CONTENT_TYPE, wildcardTemplate);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure()
            .key("POLICY_ERROR_KEY")
            .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        spyRequestHeaders.add(ACCEPT, Collections.singletonList(MediaType.APPLICATION_XML));
        ctx.setAttribute(ExecutionContext.ATTR_PREFIX + "failure", failure);

        processor.execute(ctx).test().assertResult();

        verify(mockResponse, times(1)).status(HttpStatusCode.BAD_GATEWAY_502);
    }

    @Test
    public void shouldFallbackToDefaultHandlerWithProcessorFailureKeyAndAcceptHeader() {
        ResponseTemplate jsonTemplate = new ResponseTemplate();
        jsonTemplate.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, jsonTemplate);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure()
            .key("POLICY_ERROR_KEY")
            .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        spyRequestHeaders.add(ACCEPT, Collections.singletonList(MediaType.APPLICATION_JSON));
        ctx.setAttribute(ExecutionContext.ATTR_PREFIX + "failure", failure);

        processor.execute(ctx).test().assertResult();

        verify(mockResponse, times(1)).status(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void shouldFallbackToDefaultHandlerWithProcessorFailureKeyAndAcceptHeaderAndNextMediaType() {
        ResponseTemplate jsonTemplate = new ResponseTemplate();
        jsonTemplate.setStatusCode(HttpStatusCode.BAD_REQUEST_400);

        Map<String, ResponseTemplate> mapTemplates = new HashMap<>();
        mapTemplates.put(MediaType.APPLICATION_JSON, jsonTemplate);

        Map<String, Map<String, ResponseTemplate>> templates = new HashMap<>();
        templates.put("POLICY_ERROR_KEY", mapTemplates);

        processor = new ResponseTemplateBasedFailureProcessor(templates);

        // Set failure
        DummyProcessorFailure failure = new DummyProcessorFailure()
            .key("POLICY_ERROR_KEY")
            .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);

        spyRequestHeaders.add(ACCEPT, List.of("text/html", " application/json", "*/*;q=0.8", "application/xml;q=0.9"));
        ctx.setAttribute(ExecutionContext.ATTR_PREFIX + "failure", failure);

        processor.execute(ctx).test().assertResult();

        verify(mockResponse, times(1)).status(HttpStatusCode.BAD_REQUEST_400);
    }
}
