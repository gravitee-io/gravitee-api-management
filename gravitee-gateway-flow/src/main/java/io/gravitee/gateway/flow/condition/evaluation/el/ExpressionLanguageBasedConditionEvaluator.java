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
package io.gravitee.gateway.flow.condition.evaluation.el;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.flow.condition.ConditionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link ConditionEvaluator} evaluates to true if the condition of the flow is evaluated to <code>true</code>.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExpressionLanguageBasedConditionEvaluator implements ConditionEvaluator {

    private final Logger logger = LoggerFactory.getLogger(ExpressionLanguageBasedConditionEvaluator.class);

    @Override
    public boolean evaluate(Flow flow, ExecutionContext context) {
        if (flow.getCondition() != null && !flow.getCondition().isEmpty()) {
            try {
                return context.getTemplateEngine().getValue(flow.getCondition(), Boolean.class);
            } catch (ExpressionEvaluationException ex) {
                logger.warn("EL condition could not be evaluate: {}", ex.getMessage());
                return false;
            }
        }
        return true;
    }
}
