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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.template;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;

import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.handlers.api.processor.error.template.EvaluableExecutionFailure;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.AbstractFailureMessageProcessor;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Single;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTemplateBasedFailureMessageProcessor extends AbstractFailureMessageProcessor {

    public static final String ID = "response-template-failure-message-processor";
    static final String WILDCARD_CONTENT_TYPE = "*/*";
    static final String DEFAULT_RESPONSE_TEMPLATE = "DEFAULT";

    private ResponseTemplateBasedFailureMessageProcessor() {}

    public static ResponseTemplateBasedFailureMessageProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected Single<Message> buildMessage(final MessageExecutionContext ctx, final ExecutionFailure executionFailure) {
        if (executionFailure.key() != null) {
            io.gravitee.definition.model.v4.Api api = ctx.getComponent(io.gravitee.definition.model.v4.Api.class);
            Map<String, Map<String, ResponseTemplate>> templates = api.getResponseTemplates();

            Map<String, ResponseTemplate> responseTemplates = templates.get(executionFailure.key());

            // No template associated to the error key, process the error message as usual
            if (responseTemplates == null) {
                // Try to fall back to default response template
                Map<String, ResponseTemplate> defaultResponseTemplate = templates.get(DEFAULT_RESPONSE_TEMPLATE);
                if (defaultResponseTemplate != null) {
                    return handleAcceptHeader(ctx, defaultResponseTemplate, executionFailure);
                } else {
                    return super.buildMessage(ctx, executionFailure);
                }
            } else {
                return handleAcceptHeader(ctx, responseTemplates, executionFailure);
            }
        } else {
            // No error key, process the error message as usual
            return super.buildMessage(ctx, executionFailure);
        }
    }

    private Single<Message> handleAcceptHeader(
        final MessageExecutionContext context,
        final Map<String, ResponseTemplate> templates,
        final ExecutionFailure executionFailure
    ) {
        // Extract the content-type from the request
        final List<MediaType> acceptMediaTypes = MediaType.parseMediaTypes(context.request().headers().getAll(HttpHeaderNames.ACCEPT));

        // If no accept header, check if there is a template for type matching '*/*'
        if (acceptMediaTypes == null || acceptMediaTypes.isEmpty()) {
            return handleWildcardTemplate(context, templates, executionFailure);
        } else {
            // Check against the accepted media types from incoming request sort by the quality factor
            MediaType.sortByQualityValue(acceptMediaTypes);

            for (MediaType type : acceptMediaTypes) {
                ResponseTemplate template = templates.get(type.toMediaString());

                if (template != null) {
                    return handleTemplate(context, template, executionFailure);
                }
            }

            // No template matching the accepted media types, fallback to wildcard
            return handleWildcardTemplate(context, templates, executionFailure);
        }
    }

    private Single<Message> handleWildcardTemplate(
        final MessageExecutionContext context,
        final Map<String, ResponseTemplate> templates,
        final ExecutionFailure executionFailure
    ) {
        ResponseTemplate template = templates.get(WILDCARD_CONTENT_TYPE);
        if (template == null) {
            // No template associated to the error key, process the error message as usual
            return super.buildMessage(context, executionFailure);
        } else {
            return handleTemplate(context, template, executionFailure);
        }
    }

    private Single<Message> handleTemplate(
        final MessageExecutionContext context,
        final ResponseTemplate template,
        final ExecutionFailure executionFailure
    ) {
        DefaultMessage.DefaultMessageBuilder messageBuilder = DefaultMessage.builder().id(UUID.randomUUID().toString()).error(true);
        messageBuilder.attributes(Map.of(ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure));
        Map<String, Object> metadata = new HashMap<>();
        if (executionFailure.key() != null) {
            metadata.put("key", executionFailure.key());
        }
        metadata.put("statusCode", template.getStatusCode());
        messageBuilder.metadata(metadata);

        HttpHeaders httpHeaders = HttpHeaders.create();
        if (template.getHeaders() != null) {
            template.getHeaders().forEach(httpHeaders::set);
        }

        if (template.getBody() != null && !template.getBody().isEmpty()) {
            // Prepare templating context
            final TemplateEngine templateEngine = context.getTemplateEngine();
            templateEngine.getTemplateContext().setVariable("error", new EvaluableExecutionFailure(executionFailure));

            if (executionFailure.parameters() != null && !executionFailure.parameters().isEmpty()) {
                templateEngine.getTemplateContext().setVariable("parameters", executionFailure.parameters());
            }

            // Apply templating
            return templateEngine
                .eval(template.getBody(), String.class)
                .switchIfEmpty(Single.just(""))
                .flatMap(
                    body -> {
                        Buffer payload = Buffer.buffer(body);
                        httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(payload.length()));
                        return Single.just(messageBuilder.headers(httpHeaders).content(payload).build());
                    }
                );
        }
        return Single.just(messageBuilder.headers(httpHeaders).build());
    }

    private static class Holder {

        private static final ResponseTemplateBasedFailureMessageProcessor INSTANCE = new ResponseTemplateBasedFailureMessageProcessor();
    }
}
