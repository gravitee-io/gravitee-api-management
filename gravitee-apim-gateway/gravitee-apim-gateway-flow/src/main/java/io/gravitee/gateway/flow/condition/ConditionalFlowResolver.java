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
package io.gravitee.gateway.flow.condition;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.flow.AbstractFlowResolver;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ConditionalFlowResolver extends AbstractFlowResolver {

    private final ConditionEvaluator<String> evaluator;

    public ConditionalFlowResolver(ConditionEvaluator<String> evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public List<Flow> resolve(ExecutionContext context) {
        return resolve0(context)
            .stream()
            .filter(Flow::isEnabled)
            .filter(flow -> evaluator.evaluate(flow.getCondition(), context))
            .collect(Collectors.toList());
    }

    protected abstract List<Flow> resolve0(ExecutionContext context);
}
