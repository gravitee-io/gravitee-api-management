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
package io.gravitee.gateway.reactive.handlers.api.processor.error.template;

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.processor.error.AbstractFailureProcessor;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTemplateBasedFailureProcessor extends AbstractFailureProcessor {

    public static final String ID = "response-template-failure-processor";
    static final String WILDCARD_CONTENT_TYPE = "*/*";
    static final String DEFAULT_RESPONSE_TEMPLATE = "DEFAULT";

    private ResponseTemplateBasedFailureProcessor() {
        super();
    }

    public static ResponseTemplateBasedFailureProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected Completable processFailure(final HttpPlainExecutionContext ctx, final ExecutionFailure executionFailure) {
        if (executionFailure.key() != null) {
            Map<String, Map<String, ResponseTemplate>> templates = getResponseTemplate(ctx);

            Map<String, ResponseTemplate> responseTemplates = templates.get(executionFailure.key());

            // No template associated to the error key, process the error message as usual
            if (responseTemplates == null) {
                // Try to fallback to default response template
                Map<String, ResponseTemplate> defaultResponseTemplate = templates.get(DEFAULT_RESPONSE_TEMPLATE);
                if (defaultResponseTemplate != null) {
                    return handleAcceptHeader(ctx, defaultResponseTemplate, executionFailure);
                } else {
                    return super.processFailure(ctx, executionFailure);
                }
            } else {
                return handleAcceptHeader(ctx, responseTemplates, executionFailure);
            }
        } else {
            // No error key, process the error message as usual
            return super.processFailure(ctx, executionFailure);
        }
    }

    private Map<String, Map<String, ResponseTemplate>> getResponseTemplate(final HttpPlainExecutionContext ctx) {
        try {
            io.gravitee.definition.model.v4.Api api = ctx.getComponent(io.gravitee.definition.model.v4.Api.class);
            return api.getResponseTemplates();
        } catch (Exception e) {
            io.gravitee.definition.model.Api api = ctx.getComponent(io.gravitee.definition.model.Api.class);
            return api.getResponseTemplates();
        }
    }

    private Completable handleAcceptHeader(
        final HttpPlainExecutionContext context,
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

    private Completable handleWildcardTemplate(
        final HttpPlainExecutionContext context,
        final Map<String, ResponseTemplate> templates,
        final ExecutionFailure executionFailure
    ) {
        ResponseTemplate template = templates.get(WILDCARD_CONTENT_TYPE);
        if (template == null) {
            // No template associated to the error key, process the error message as usual
            return super.processFailure(context, executionFailure);
        } else {
            return handleTemplate(context, template, executionFailure);
        }
    }

    private Completable handleTemplate(
        final HttpPlainExecutionContext context,
        final ResponseTemplate template,
        final ExecutionFailure executionFailure
    ) {
        context.response().status(template.getStatusCode());
        context.response().reason(HttpResponseStatus.valueOf(template.getStatusCode()).reasonPhrase());
        context.response().headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        if (template.isPropagateErrorKeyToLogs()) {
            context.metrics().setErrorKey(executionFailure.key());
        }

        if (template.getBody() != null && !template.getBody().isEmpty()) {
            context.response().headers().set(HttpHeaderNames.CONTENT_TYPE, context.request().headers().get(HttpHeaderNames.ACCEPT));
        }

        if (template.getHeaders() != null) {
            template.getHeaders().forEach((name, value) -> context.response().headers().set(name, value));
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
                .flatMapCompletable(body -> {
                    Buffer payload = Buffer.buffer(body);
                    context.response().headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(payload.length()));
                    context.response().body(payload);
                    return Completable.complete();
                });
        }
        return Completable.complete();
    }

    private static class Holder {

        private static final ResponseTemplateBasedFailureProcessor INSTANCE = new ResponseTemplateBasedFailureProcessor();
    }
}
