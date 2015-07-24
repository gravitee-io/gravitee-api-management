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
package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.policy.PolicyConfiguration;
import io.gravitee.gateway.api.policy.annotations.OnRequest;
import io.gravitee.gateway.api.policy.annotations.OnResponse;
import io.gravitee.gateway.core.policy.impl.PolicyFactoryImpl;
import io.gravitee.gateway.core.policy.impl.PolicyImpl;
import org.junit.Before;
import org.junit.Test;
import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyTest {

    private PolicyFactory policyFactory;

    @Before
    public void setUp() {
        policyFactory = new PolicyFactoryImpl();
    }

    @Test
    public void test() throws Exception {
        PolicyDefinition policyDefinition = getPolicyDefinition();

        Object policyInst = policyFactory.create(policyDefinition, null);
        Method onRequestMethod = resolvePolicyMethod(policyDefinition.policy(), OnRequest.class);
        Method onResponseMethod = resolvePolicyMethod(policyDefinition.policy(), OnResponse.class);

        Policy policy = new PolicyImpl(policyInst, onRequestMethod, onResponseMethod);
        policy.onRequest();
    }

    private Method resolvePolicyMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Method> methods = ReflectionUtils.getMethods(
                clazz,
                withModifier(Modifier.PUBLIC),
                withAnnotation(annotationClass));

        if (methods.isEmpty()) {
            return null;
        }

        return methods.iterator().next();
    }

    private PolicyDefinition getPolicyDefinition() {
        return new PolicyDefinition() {
            @Override
            public String id() {
                return null;
            }

            @Override
            public String name() {
                return "my-policy";
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String version() {
                return null;
            }

            @Override
            public Class<?> policy() {
                return DummyPolicy.class;
            }

            @Override
            public Class<? extends PolicyConfiguration> configuration() {
                return null;
            }

            @Override
            public List<URL> getClassPathElements() {
                return null;
            }

            @Override
            public Method onRequestMethod() {
                return null;
            }

            @Override
            public Method onResponseMethod() {
                return null;
            }
        };
    }
}
