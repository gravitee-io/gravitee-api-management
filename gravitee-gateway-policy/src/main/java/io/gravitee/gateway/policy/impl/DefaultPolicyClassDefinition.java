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

import io.gravitee.gateway.policy.PolicyClassDefinition;
import io.gravitee.policy.api.PolicyConfiguration;

import java.lang.reflect.Method;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class DefaultPolicyClassDefinition implements PolicyClassDefinition {

    private String id;
    private Class<? extends PolicyConfiguration> configuration;
    private Class<?> policy;
    private Method onRequestMethod, onResponseMethod, onRequestContentMethod, onResponseContentMethod;

    public DefaultPolicyClassDefinition(String id) {
        this.id = id;
    }

    @Override
    public Class<? extends PolicyConfiguration> configuration() {
        return configuration;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Class<?> policy() {
        return policy;
    }

    @Override
    public Method onRequestMethod() {
        return onRequestMethod;
    }

    @Override
    public Method onRequestContentMethod() {
        return onRequestContentMethod;
    }

    @Override
    public Method onResponseMethod() {
        return onResponseMethod;
    }

    @Override
    public Method onResponseContentMethod() {
        return onResponseContentMethod;
    }

    public void setConfiguration(Class<? extends PolicyConfiguration> configuration) {
        this.configuration = configuration;
    }

    public void setOnRequestContentMethod(Method onRequestContentMethod) {
        this.onRequestContentMethod = onRequestContentMethod;
    }

    public void setOnRequestMethod(Method onRequestMethod) {
        this.onRequestMethod = onRequestMethod;
    }

    public void setOnResponseContentMethod(Method onResponseContentMethod) {
        this.onResponseContentMethod = onResponseContentMethod;
    }

    public void setOnResponseMethod(Method onResponseMethod) {
        this.onResponseMethod = onResponseMethod;
    }

    public void setPolicy(Class<?> policy) {
        this.policy = policy;
    }
}
