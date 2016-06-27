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
package io.gravitee.gateway.el;

import io.gravitee.gateway.api.expression.TemplateContext;
import io.gravitee.gateway.el.function.JsonPathFunction;
import org.springframework.beans.BeanUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class SpelTemplateContext implements TemplateContext {

    private final StandardEvaluationContext context = new StandardEvaluationContext();

    public SpelTemplateContext() {
        context.registerFunction("jsonPath",
                BeanUtils.resolveSignature("evaluate", JsonPathFunction.class));
    }

    @Override
    public void setVariable(String name, Object value) {
        context.setVariable(name, value);
    }

    @Override
    public Object lookupVariable(String name) {
        return context.lookupVariable(name);
    }

    public EvaluationContext getContext() {
        return context;
    }
}
