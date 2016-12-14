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
package io.gravitee.gateway.policy.impl;

import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.PolicyContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyMetadataBuilder {

    private String id;

    private Class<? extends PolicyConfiguration> configuration;

    private PolicyContext context;

    private Class<?> policy;

    private Map<Class<? extends Annotation>, Method> methods;

    public PolicyMetadataBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public PolicyMetadataBuilder setConfiguration(Class<? extends PolicyConfiguration> configuration) {
        this.configuration = configuration;
        return this;
    }

    public PolicyMetadataBuilder setContext(PolicyContext context) {
        this.context = context;
        return this;
    }

    public PolicyMetadataBuilder setPolicy(Class<?> policy) {
        this.policy = policy;
        return this;
    }

    public PolicyMetadataBuilder setMethods(Map<Class<? extends Annotation>, Method> methods) {
        this.methods = methods;
        return this;
    }

    public PolicyMetadata build() {
        return new PolicyMetadata() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Class<?> policy() {
                return policy;
            }

            @Override
            public Class<? extends PolicyConfiguration> configuration() {
                return configuration;
            }

            @Override
            public PolicyContext context() {
                return context;
            }

            @Override
            public Method method(Class<? extends Annotation> type) {
                return methods.get(type);
            }
        };
    }
}
