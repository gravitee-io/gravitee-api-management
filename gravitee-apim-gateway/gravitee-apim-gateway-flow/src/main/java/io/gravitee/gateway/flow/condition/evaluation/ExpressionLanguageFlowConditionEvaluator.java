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
package io.gravitee.gateway.flow.condition.evaluation;

import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;

/**
 * This {@link ConditionEvaluator} evaluates to true if the condition of the string is evaluated to <code>true</code>.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExpressionLanguageFlowConditionEvaluator implements ConditionEvaluator<FlowV2> {

    @Override
    public boolean evaluate(ExecutionContext context, FlowV2 flow) {
        if (flow.getCondition() != null && !flow.getCondition().isEmpty()) {
            return context.getTemplateEngine().getValue(flow.getCondition(), Boolean.class);
        }

        return true;
    }
}
