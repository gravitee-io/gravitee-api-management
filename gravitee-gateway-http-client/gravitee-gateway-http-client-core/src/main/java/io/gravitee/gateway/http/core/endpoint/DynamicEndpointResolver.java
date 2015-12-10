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
package io.gravitee.gateway.http.core.endpoint;

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.http.core.endpoint.parser.TemplateParserContext;
import io.gravitee.gateway.http.core.endpoint.parser.WrappedRequestVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.net.URI;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DynamicEndpointResolver extends AbstractEndpointResolver {

    private final Logger logger = LoggerFactory.getLogger(DynamicEndpointResolver.class);

    private EvaluationContext context;
    private Expression expression;

    protected DynamicEndpointResolver(Api api) {
        super(api);

        initialize();
    }

    private void initialize() {
        String endpoint = getEndpoint();
        logger.info("Initializing parser for dynamic endpoint: {}", endpoint);

        try {
            context = new StandardEvaluationContext();
            expression = new SpelExpressionParser().
                    parseExpression(endpoint, new TemplateParserContext());

            context.setVariable("properties", api.getProperties());
        } catch (ParseException pe) {
                logger.error("An error occurs while parsing expression for dynamic endpoint", pe);
            throw new IllegalStateException("Unable to parse expression for dynamic endpoint: " + endpoint);
        }
    }

    public URI resolve(Request request) {
        context.setVariable("request", new WrappedRequestVariable(request));

        return URI.create(expression.getValue(context, String.class));
    }
}
