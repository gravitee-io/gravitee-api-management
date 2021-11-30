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
package io.gravitee.gateway.core.endpoint.ref.impl;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.endpoint.EndpointManager;
import io.gravitee.gateway.core.endpoint.ref.Reference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@TemplateVariable(scopes = { TemplateVariableScope.API })
public class DefaultReferenceRegister implements EndpointManager, ReferenceRegister, TemplateVariableProvider {

    private static final String TEMPLATE_VARIABLE_KEY = "endpoints";

    private final Map<String, Reference> references = new HashMap<>();

    @Override
    public void add(Reference reference) {
        references.put(reference.name(), reference);
    }

    @Override
    public void remove(String reference) {
        references.remove(reference);
    }

    @Override
    public Reference lookup(String reference) {
        return references.get(reference);
    }

    @Override
    public Collection<Reference> references() {
        return references.values();
    }

    @Override
    public <T extends Reference> Collection<T> referencesByType(Class<T> refClass) {
        return references()
            .stream()
            .filter(reference -> reference.getClass().equals(refClass))
            .map(reference -> (T) reference)
            .collect(Collectors.toSet());
    }

    @Override
    public void provide(TemplateContext context) {
        Map<String, String> refs = references
            .entrySet()
            .stream()
            .collect(Collectors.toMap(entry -> entry.getValue().name(), entry -> entry.getKey() + ':'));

        context.setVariable(TEMPLATE_VARIABLE_KEY, new EndpointReferenceMap(refs));
    }

    @Override
    public Endpoint get(String name) {
        Reference reference = lookup(name);
        return (reference != null) ? reference.endpoint() : null;
    }

    private static class EndpointReferenceMap implements Map<String, String> {

        private final Map<String, String> wrapped;

        EndpointReferenceMap(Map<String, String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int size() {
            return wrapped.size();
        }

        @Override
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return wrapped.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return wrapped.containsValue(value);
        }

        @Override
        public String get(Object key) {
            String reference = wrapped.get(key);
            return (reference != null) ? reference : Reference.UNKNOWN_REFERENCE;
        }

        @Override
        public String put(String key, String value) {
            return wrapped.put(key, value);
        }

        @Override
        public String remove(Object key) {
            return wrapped.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m) {
            wrapped.putAll(m);
        }

        @Override
        public void clear() {
            wrapped.clear();
        }

        @Override
        public Set<String> keySet() {
            return wrapped.keySet();
        }

        @Override
        public Collection<String> values() {
            return wrapped.values();
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return wrapped.entrySet();
        }

        @Override
        public boolean equals(Object o) {
            return wrapped.equals(o);
        }

        @Override
        public int hashCode() {
            return wrapped.hashCode();
        }

        @Override
        public String getOrDefault(Object key, String defaultValue) {
            return wrapped.getOrDefault(key, defaultValue);
        }

        @Override
        public void forEach(BiConsumer<? super String, ? super String> action) {
            wrapped.forEach(action);
        }

        @Override
        public void replaceAll(BiFunction<? super String, ? super String, ? extends String> function) {
            wrapped.replaceAll(function);
        }

        @Override
        public String putIfAbsent(String key, String value) {
            return wrapped.putIfAbsent(key, value);
        }

        @Override
        public boolean remove(Object key, Object value) {
            return wrapped.remove(key, value);
        }

        @Override
        public boolean replace(String key, String oldValue, String newValue) {
            return wrapped.replace(key, oldValue, newValue);
        }

        @Override
        public String replace(String key, String value) {
            return wrapped.replace(key, value);
        }

        @Override
        public String computeIfAbsent(String key, Function<? super String, ? extends String> mappingFunction) {
            return wrapped.computeIfAbsent(key, mappingFunction);
        }

        @Override
        public String computeIfPresent(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
            return wrapped.computeIfPresent(key, remappingFunction);
        }

        @Override
        public String compute(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
            return wrapped.compute(key, remappingFunction);
        }

        @Override
        public String merge(String key, String value, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
            return wrapped.merge(key, value, remappingFunction);
        }
    }
}
