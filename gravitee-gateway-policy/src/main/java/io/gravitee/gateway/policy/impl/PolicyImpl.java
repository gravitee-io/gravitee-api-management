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

import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyImpl implements Policy {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyImpl.class);

    private final Object policyInst;
    private Method onRequestMethod, onResponseMethod, onRequestContentMethod, onResponseContentMethod;

    private PolicyImpl(Object policyInst) {
        this.policyInst = policyInst;
    }

    @Override
    public void onRequest(Object ... args) throws Exception {
        if (onRequestMethod != null) {
            invoke(onRequestMethod, args);
        }
    }

    @Override
    public void onResponse(Object ... args) throws Exception {
        if (onResponseMethod != null) {
            invoke(onResponseMethod, args);
        }
    }

    @Override
    public ReadWriteStream<?> onResponseContent(Object ... args) throws Exception {
        if (onResponseContentMethod != null) {
            return (ReadWriteStream) invoke(onResponseContentMethod, args);
        }

        return null;
    }

    @Override
    public ReadWriteStream<?> onRequestContent(Object ... args) throws Exception {
        if (onRequestContentMethod != null) {
            return (ReadWriteStream) invoke(onRequestContentMethod, args);
        }

        return null;
    }

    private PolicyImpl onRequestContentMethod(Method onRequestContentMethod) {
        this.onRequestContentMethod = onRequestContentMethod;
        return this;
    }

    private PolicyImpl onRequestMethod(Method onRequestMethod) {
        this.onRequestMethod = onRequestMethod;
        return this;
    }

    private PolicyImpl onResponseContentMethod(Method onResponseContentMethod) {
        this.onResponseContentMethod = onResponseContentMethod;
        return this;
    }

    private PolicyImpl onResponseMethod(Method onResponseMethod) {
        this.onResponseMethod = onResponseMethod;
        return this;
    }

    private Object invoke(Method invokedMethod, Object ... args) throws Exception {
        LOGGER.debug("Calling {} method on policy {}", invokedMethod.getName(), policyInst.getClass().getName());

        Class<?>[] parametersType = invokedMethod.getParameterTypes();
        Object[] parameters = new Object[parametersType.length];

        int idx = 0;

        // Map parameters according to parameter's type
        for(Class<?> paramType : parametersType) {
            parameters[idx++] = getParameterAssignableTo(paramType, args);
        }

        return invokedMethod.invoke(policyInst, parameters);
    }

    private Object getParameterAssignableTo(Class<?> paramType, Object ... args) {
        for(Object arg: args) {
            if (paramType.isAssignableFrom(arg.getClass())) {
                return arg;
            }
        }

        return null;
    }

    public static Builder with(Object policyInstance) {
        return new Builder(policyInstance);
    }

    public static class Builder {

        private final Object policyInstance;
        private Method onRequestMethod, onResponseMethod, onRequestContentMethod, onResponseContentMethod;

        private Builder(Object policyInstance) {
            this.policyInstance = policyInstance;
        }

        public Builder onRequestContentMethod(Method onRequestContentMethod) {
            this.onRequestContentMethod = onRequestContentMethod;
            return this;
        }

        public Builder onRequestMethod(Method onRequestMethod) {
            this.onRequestMethod = onRequestMethod;
            return this;
        }

        public Builder onResponseContentMethod(Method onResponseContentMethod) {
            this.onResponseContentMethod = onResponseContentMethod;
            return this;
        }

        public Builder onResponseMethod(Method onResponseMethod) {
            this.onResponseMethod = onResponseMethod;
            return this;
        }

        public PolicyImpl build() {
            return new PolicyImpl(policyInstance)
                    .onRequestContentMethod(onRequestContentMethod)
                    .onRequestMethod(onRequestMethod)
                    .onResponseContentMethod(onResponseContentMethod)
                    .onResponseMethod(onResponseMethod);
        }
    }
}
