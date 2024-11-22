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
package io.gravitee.gateway.reactive.policy.adapter.context;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.reactivex.rxjava3.core.Maybe;

/**
 * {@link TemplateEngine} adapter that uses a {@link TemplateContextAdapter} allowing to restore v4 engine's variables that may have been overridden during policy or invoker execution.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TemplateEngineAdapter implements TemplateEngine {

    private final TemplateEngine templateEngine;
    private TemplateContextAdapter adaptedTemplateContext;

    public TemplateEngineAdapter(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String convert(String expression) {
        return templateEngine.convert(expression);
    }

    @Override
    public <T> T getValue(String expression, Class<T> clazz) {
        return templateEngine.getValue(expression, clazz);
    }

    @Override
    public <T> T evalNow(String expression, Class<T> clazz) {
        return templateEngine.evalNow(expression, clazz);
    }

    @Override
    public <T> Maybe<T> eval(String expression, Class<T> clazz) {
        return templateEngine.eval(expression, clazz);
    }

    @Override
    public TemplateContext getTemplateContext() {
        if (adaptedTemplateContext == null) {
            adaptedTemplateContext = new TemplateContextAdapter(templateEngine.getTemplateContext());
        }
        return adaptedTemplateContext;
    }

    void restore() {
        if (adaptedTemplateContext != null) {
            adaptedTemplateContext.restore();
        }
    }
}
