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

import io.gravitee.gateway.api.expression.TemplateContext;
import io.gravitee.gateway.api.expression.TemplateVariableProvider;
import io.gravitee.gateway.core.endpoint.ref.Reference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReferenceRegister implements ReferenceRegister, TemplateVariableProvider {

    private final static String TEMPLATE_VARIABLE_KEY = "endpoints";

    private final Map<String, Reference> references = new HashMap<>();

    @Override
    public void add(Reference reference) {
        references.put(reference.key(), reference);
    }

    @Override
    public void remove(String reference) {
        references.remove(reference);
    }

    @Override
    public Reference get(String reference) {
        return references.get(reference);
    }

    @Override
    public Collection<Reference> references() {
        return references.values();
    }

    @Override
    public Collection<Reference> referencesByType(Class<? extends Reference> refClass) {
        return references()
                .stream()
                .filter(reference -> reference.getClass().equals(refClass))
                .collect(Collectors.toSet());
    }

    @Override
    public void provide(TemplateContext context) {
        Map<String, String> refs = references.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getValue().name(),
                        entry -> entry.getKey() + ':'));

        context.setVariable(TEMPLATE_VARIABLE_KEY, refs);
    }
}
