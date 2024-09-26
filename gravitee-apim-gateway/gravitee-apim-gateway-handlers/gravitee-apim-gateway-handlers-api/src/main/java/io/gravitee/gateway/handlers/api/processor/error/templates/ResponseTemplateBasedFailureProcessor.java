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
package io.gravitee.gateway.handlers.api.processor.error.templates;

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.el.EvaluableRequest;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.handlers.api.processor.error.SimpleFailureProcessor;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTemplateBasedFailureProcessor extends SimpleFailureProcessor {

    static final String WILDCARD_CONTENT_TYPE = "*/*";
    static final String DEFAULT_RESPONSE_TEMPLATE = "DEFAULT";

    private final Map<String, Map<String, ResponseTemplate>> templates;

    public ResponseTemplateBasedFailureProcessor(final Map<String, Map<String, ResponseTemplate>> templates) {
        this.templates = templates;
    }

    @Override
    protected void handleFailure(ExecutionContext context, ProcessorFailure failure) {
        if (failure.key() != null) {
            Map<String, ResponseTemplate> responseTemplates = templates.get(failure.key());

            // No template associated to the error key, process the error message as usual
            if (responseTemplates == null) {
                // Try to fallback to default response template

                Map<String, ResponseTemplate> defaultResponseTemplate = templates.get(DEFAULT_RESPONSE_TEMPLATE);
                if (defaultResponseTemplate != null) {
                    handleAcceptHeader(context, defaultResponseTemplate, failure);
                } else {
                    super.handleFailure(context, failure);
                }
            } else {
                handleAcceptHeader(context, responseTemplates, failure);
            }
        } else {
            // No error key, process the error message as usual
            super.handleFailure(context, failure);
        }
    }

    private void handleAcceptHeader(
        final ExecutionContext context,
        final Map<String, ResponseTemplate> templates,
        final ProcessorFailure failure
    ) {
        // Extract the content-type from the request
        final List<MediaType> acceptMediaTypes = MediaType.parseMediaTypes(context.request().headers().getAll(HttpHeaderNames.ACCEPT));

        // If no accept header, check if there is a template for type matching '*/*'
        if (acceptMediaTypes == null || acceptMediaTypes.isEmpty()) {
            handleWildcardTemplate(context, templates, failure);
        } else {
            // Check against the accepted media types from incoming request sort by the quality factor
            MediaType.sortByQualityValue(acceptMediaTypes);

            for (MediaType type : acceptMediaTypes) {
                ResponseTemplate template = templates.get(type.toMediaString());

                if (template != null) {
                    handleTemplate(context, template, failure);
                    return;
                }
            }

            // No template matching the accepted media types, fallback to wildcard
            handleWildcardTemplate(context, templates, failure);
        }
    }

    private void handleWildcardTemplate(
        final ExecutionContext context,
        final Map<String, ResponseTemplate> templates,
        final ProcessorFailure failure
    ) {
        ResponseTemplate template = templates.get(WILDCARD_CONTENT_TYPE);
        if (template == null) {
            // No template associated to the error key, process the error message as usual
            super.handleFailure(context, failure);
        } else {
            handleTemplate(context, template, failure);
        }
    }

    private void handleTemplate(final ExecutionContext context, final ResponseTemplate template, final ProcessorFailure failure) {
        context.response().status(template.getStatusCode());
        context.response().reason(HttpResponseStatus.valueOf(template.getStatusCode()).reasonPhrase());
        context.response().headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        if (template.isPropagateErrorKeyToLogs()) {
            context.request().metrics().setErrorKey(failure.key());
        }

        if (template.getBody() != null && !template.getBody().isEmpty()) {
            context.response().headers().set(HttpHeaderNames.CONTENT_TYPE, context.request().headers().getFirst(HttpHeaderNames.ACCEPT));
        }

        if (template.getHeaders() != null) {
            template.getHeaders().forEach((name, value) -> context.response().headers().set(name, value));
        }

        if (template.getBody() != null && !template.getBody().isEmpty()) {
            // Prepare templating context
            context.getTemplateEngine().getTemplateContext().setVariable("error", new EvaluableProcessorFailure(failure));

            context.getTemplateEngine().getTemplateContext().setVariable("request", new EvaluableRequest(context.request()));

            if (failure.parameters() != null && !failure.parameters().isEmpty()) {
                context.getTemplateEngine().getTemplateContext().setVariable("parameters", failure.parameters());
            }
            // Apply templating
            String body = context.getTemplateEngine().getValue(template.getBody(), String.class);
            Buffer payload = Buffer.buffer(body);
            context.response().headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(payload.length()));
            context.response().write(payload);
        }
    }
}
