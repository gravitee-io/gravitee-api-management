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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.common.util.ListUtils;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public abstract class AbstractBaseExecutionContext implements BaseExecutionContext {

    protected Map<String, Object> attributes = new ContextAttributeMap();
    protected Map<String, Object> internalAttributes = new HashMap<>();
    protected ComponentProvider componentProvider;
    protected TemplateEngine templateEngine;

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return componentProvider.getComponent(componentClass);
    }

    @Override
    public void setAttribute(String name, Object value) {
        putAttribute(name, value);
    }

    @Override
    public void putAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    @Override
    public <T> List<T> getAttributeAsList(String name) {
        return ListUtils.toList(attributes.get(name));
    }

    @Override
    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    @Override
    public <T> Map<String, T> getAttributes() {
        return (Map<String, T>) this.attributes;
    }

    @Override
    public void setInternalAttribute(String name, Object value) {
        putInternalAttribute(name, value);
    }

    @Override
    public void putInternalAttribute(String name, Object value) {
        internalAttributes.put(name, value);
    }

    @Override
    public void removeInternalAttribute(String name) {
        internalAttributes.remove(name);
    }

    @Override
    public <T> T getInternalAttribute(String name) {
        return (T) internalAttributes.get(name);
    }

    @Override
    public <T> Map<String, T> getInternalAttributes() {
        return (Map<String, T>) internalAttributes;
    }

    @Override
    public abstract TemplateEngine getTemplateEngine();
}
