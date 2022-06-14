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
package io.gravitee.gateway.jupiter.policy.adapter.context;

import io.gravitee.el.TemplateContext;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link TemplateContext} adapter that can be passed to V3 policy or invoker and can be restore any variables that may have been overridden.
 * This is especially the case when some existing policies are replacing {@link io.gravitee.gateway.jupiter.api.el.EvaluableRequest} or {@link io.gravitee.gateway.jupiter.api.el.EvaluableResponse}
 * with their own instance making impossible for other policies to benefit from Jupiter enhancements.
 *
 * This adapter keeps track of all variables that are replaced and allows to restore them by simply calling the {@link #restore()} method
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TemplateContextAdapter implements TemplateContext {

    private final TemplateContext templateContext;
    private Map<String, Object> backupVariables;

    public TemplateContextAdapter(TemplateContext templateContext) {
        this.templateContext = templateContext;
    }

    @Override
    public void setVariable(String name, Object value) {
        backupVariable(name);
        templateContext.setVariable(name, value);
    }

    @Override
    public void setDeferredVariable(String name, Completable deferred) {
        // Should never be called in a V3 context.
    }

    @Override
    public void setDeferredVariable(String name, Maybe<?> deferred) {
        // Should never be called in a V3 context.
    }

    @Override
    public void setDeferredVariable(String name, Single<?> deferred) {
        // Should never be called in a V3 context.
    }

    @Override
    public Object lookupVariable(String name) {
        return templateContext.lookupVariable(name);
    }

    void restore() {
        if (backupVariables != null) {
            backupVariables.forEach(templateContext::setVariable);
            backupVariables.clear();
        }
    }

    private void backupVariable(String name) {
        final Object lookup = templateContext.lookupVariable(name);
        if (lookup != null) {
            if (backupVariables == null) {
                backupVariables = new HashMap<>();
            }

            backupVariables.putIfAbsent(name, lookup);
        }
    }
}
