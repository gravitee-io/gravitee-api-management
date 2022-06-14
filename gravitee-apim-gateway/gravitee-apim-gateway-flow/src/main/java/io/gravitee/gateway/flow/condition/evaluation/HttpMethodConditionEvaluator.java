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
package io.gravitee.gateway.flow.condition.evaluation;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;

/**
 * This {@link ConditionEvaluator} evaluates to true if the method of the request is matching the
 * methods declared within the {@link Flow}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpMethodConditionEvaluator implements ConditionEvaluator<Flow> {

    @Override
    public boolean evaluate(ExecutionContext context, Flow flow) {
        return evaluate(context.request().method(), flow);
    }

    protected boolean evaluate(HttpMethod method, Flow flow) {
        return flow.getMethods() == null || flow.getMethods().isEmpty() || flow.getMethods().contains(method);
    }
}
