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
package io.gravitee.gateway.handlers.api.context;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.expression.TemplateContext;
import io.gravitee.gateway.api.expression.TemplateEngine;
import io.gravitee.gateway.api.expression.TemplateVariableProvider;
import io.gravitee.gateway.el.SpelTemplateEngine;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestExecutionContext implements ExecutionContext {

    private final Map<String, Object> attributes = new AttributeMap();

    private final Request request;

    private final ApplicationContext applicationContext;

    private Collection<TemplateVariableProvider> providers;

    private SpelTemplateEngine spelTemplateEngine;

    RequestExecutionContext(Request request, ApplicationContext applicationContext) {
        this.request = request;
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return applicationContext.getBean(componentClass);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        if (spelTemplateEngine == null) {
            spelTemplateEngine = new SpelTemplateEngine();

            TemplateContext templateContext = spelTemplateEngine.getTemplateContext();
            templateContext.setVariable("request", new EvaluableRequest(request));
            templateContext.setVariable("context", new EvaluableExecutionContext(this));

            if (providers != null) {
                providers.forEach(templateVariableProvider -> templateVariableProvider.provide(templateContext));
            }
        }

        return spelTemplateEngine;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    void setProviders(Collection<TemplateVariableProvider> providers) {
        this.providers = providers;
    }

    private class AttributeMap extends HashMap<String, Object> {

        @Override
        public Object get(Object key) {
            Object value = super.get(key);
            return (value != null) ? value : super.get(ExecutionContext.ATTR_PREFIX + key);
        }
    }
}
