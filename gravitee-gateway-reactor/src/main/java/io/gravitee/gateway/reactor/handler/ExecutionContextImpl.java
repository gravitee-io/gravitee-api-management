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
package io.gravitee.gateway.reactor.handler;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.expression.TemplateEngine;
import io.gravitee.gateway.el.SpelTemplateEngine;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExecutionContextImpl implements ExecutionContext {

    private final Map<String, Object> attributes = new HashMap<>();

    private final ApplicationContext applicationContext;

    private final SpelTemplateEngine spelTemplateEngine = new SpelTemplateEngine();

    public ExecutionContextImpl(ApplicationContext applicationContext) {
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
        return spelTemplateEngine;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }
}
