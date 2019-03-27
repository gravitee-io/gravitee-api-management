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
package io.gravitee.gateway.handlers.api.processor.error.templates;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.ResponseTemplates;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.ProcessorFailure;
import io.gravitee.gateway.handlers.api.processor.error.SimpleFailureProcessor;

import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTemplateBasedFailureProcessor extends SimpleFailureProcessor {

    private static final String WILDCARD_CONTENT_TYPE = "*/*";

    private final Map<String, ResponseTemplates> templates;

    public ResponseTemplateBasedFailureProcessor(final Map<String, ResponseTemplates> templates) {
        this.templates = templates;
    }

    @Override
    protected void handleFailure(ExecutionContext context, ProcessorFailure failure) {
        if (failure.key() != null) {
            ResponseTemplates responseTemplates = templates.get(failure.key());

            // No template associated to the error key, process the error message as usual
            if (responseTemplates == null) {
                super.handleFailure(context, failure);
            } else {
                handleAcceptHeader(context, responseTemplates, failure);
            }
        } else {
            // No error key, process the error message as usual
            super.handleFailure(context, failure);
        }
    }

    private void handleAcceptHeader(final ExecutionContext context, final ResponseTemplates templates, final ProcessorFailure failure) {
        // Extract the content-type from the request
        String acceptHeader = context.request().headers().getFirst(HttpHeaders.ACCEPT);

        // If no accept header, check if there is a template for type matching '*/*'
        if (acceptHeader == null) {
            handleWildcardTemplate(context, templates, failure);
        } else {
            // Accept header may contain multiple
            ResponseTemplate template = templates.getTemplates().get(acceptHeader);

            // No template is matching the accept header, fallback to  wildcard
            if (template == null) {
                handleWildcardTemplate(context, templates, failure);
            } else {
                handleTemplate(context, template, failure);
            }
        }
    }

    private void handleWildcardTemplate(final ExecutionContext context, final ResponseTemplates templates, final ProcessorFailure failure) {
        ResponseTemplate template = templates.getTemplates().get(WILDCARD_CONTENT_TYPE);
        if (template == null) {
            // No template associated to the error key, process the error message as usual
            super.handleFailure(context, failure);
        } else {
            handleTemplate(context, template, failure);
        }
    }

    private void handleTemplate(final ExecutionContext context, final ResponseTemplate template, final ProcessorFailure failure) {
        context.response().status(template.getStatusCode());
        context.response().headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

        if (template.getBody() != null && !template.getBody().isEmpty()) {
            context.response().headers().set(HttpHeaders.CONTENT_TYPE, context.request().headers().getFirst(HttpHeaders.ACCEPT));
        }

        if (template.getHeaders() != null) {
            template.getHeaders().forEach((name, value) -> context.response().headers().set(name, value));
        }

        if (template.getBody() != null && !template.getBody().isEmpty()) {
            // Prepare templating context
            context.getTemplateEngine().getTemplateContext().setVariable("error",
                    new EvaluableProcessorFailure(failure));

            context.getTemplateEngine().getTemplateContext().setVariable("request",
                    new EvaluableRequest(context.request()));

            if (failure.parameters() != null && ! failure.parameters().isEmpty()) {
                context.getTemplateEngine().getTemplateContext().setVariable("parameters", failure.parameters());
            }

            // Apply templating
            String body = context.getTemplateEngine().getValue(template.getBody(), String.class);
            Buffer payload = Buffer.buffer(body);
            context.response().headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(payload.length()));
            context.response().write(payload);
        }
    }
}
