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
package io.gravitee.gateway.core.policy.impl;

import io.gravitee.gateway.api.policy.annotations.OnRequest;
import io.gravitee.gateway.api.policy.annotations.OnResponse;
import io.gravitee.gateway.core.policy.PolicyMethodResolver;
import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyMethodResolverImpl implements PolicyMethodResolver {

    private final static Class<? extends Annotation> [] resolvableAnnotations = new Class[]{
            OnRequest.class, OnResponse.class
    };

    @Override
    public Map<Class<? extends Annotation>, Method> resolvePolicyMethods(Class<?> policyClass) {
        Map<Class<? extends Annotation>, Method> methods = new HashMap<>();

        for(Class<? extends Annotation> annot : resolvableAnnotations) {
            Set<Method> resolved = ReflectionUtils.getMethods(
                    policyClass,
                    withModifier(Modifier.PUBLIC),
                    withAnnotation(annot));

            // TODO: We have to check that the method is using PolicyChain as parameter.
            if (! resolved.isEmpty()) {
                methods.put(annot, resolved.iterator().next());
            }
        }

        return methods;
    }
}
