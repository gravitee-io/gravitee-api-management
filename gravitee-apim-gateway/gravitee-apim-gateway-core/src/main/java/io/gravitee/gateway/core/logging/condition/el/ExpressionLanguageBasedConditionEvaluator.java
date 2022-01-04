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
package io.gravitee.gateway.core.logging.condition.el;

import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExpressionLanguageBasedConditionEvaluator implements ConditionEvaluator<Request> {

    public static final Logger LOGGER = LoggerFactory.getLogger(ExpressionLanguageBasedConditionEvaluator.class);

    private final String condition;

    public ExpressionLanguageBasedConditionEvaluator(final String condition) {
        this.condition = condition;
    }

    @Override
    public boolean evaluate(ExecutionContext executionContext, Request request) {
        if (condition != null) {
            try {
                return executionContext.getTemplateEngine().getValue(condition, Boolean.class);
            } catch (ExpressionEvaluationException e) {
                LOGGER.warn("Error parsing condition {}", condition, e);
                return false;
            }
        }

        return true;
    }
}
