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
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyImpl implements Policy {

    private final Object policyInst;
    private PolicyMetadata policyMetadata;

    private PolicyImpl(Object policyInst) {
        this.policyInst = policyInst;
    }

    @Override
    public void onRequest(Object ... args) throws Exception {
        invoke(policyMetadata.method(OnRequest.class), args);
    }

    @Override
    public void onResponse(Object ... args) throws Exception {
        invoke(policyMetadata.method(OnResponse.class), args);
    }

    @Override
    public ReadWriteStream<?> onResponseContent(Object ... args) throws Exception {
        Object stream = invoke(policyMetadata.method(OnResponseContent.class), args);
        return (stream != null) ? (ReadWriteStream) stream : null;
    }

    @Override
    public boolean isStreamable() {
        return (policyMetadata.method(OnRequestContent.class) != null ||
                policyMetadata.method(OnResponseContent.class) != null);
    }

    @Override
    public boolean isRunnable() {
        return (policyMetadata.method(OnRequest.class) != null ||
                policyMetadata.method(OnResponse.class) != null);
    }

    @Override
    public ReadWriteStream<?> onRequestContent(Object ... args) throws Exception {
        Object stream = invoke(policyMetadata.method(OnRequestContent.class), args);
        return (stream != null) ? (ReadWriteStream) stream : null;
    }

    private Object invoke(Method invokedMethod, Object ... args) throws Exception {
        if (invokedMethod != null) {
            Class<?>[] parametersType = invokedMethod.getParameterTypes();
            Object[] parameters = new Object[parametersType.length];

            int idx = 0;

            // Map parameters according to parameter's type
            for (Class<?> paramType : parametersType) {
                parameters[idx++] = getParameterAssignableTo(paramType, args);
            }

            return invokedMethod.invoke(policyInst, parameters);
        }

        return null;
    }

    private Object getParameterAssignableTo(Class<?> paramType, Object ... args) {
        for(Object arg: args) {
            if (paramType.isAssignableFrom(arg.getClass())) {
                return arg;
            }
        }

        return null;
    }

    public static Builder target(Object policyInstance) {
        return new Builder(policyInstance);
    }

    private PolicyImpl definition(PolicyMetadata policyMetadata) {
        this.policyMetadata = policyMetadata;
        return this;
    }

    public static class Builder {

        private final Object policyInstance;
        private PolicyMetadata policyMetadata;

        private Builder(Object policyInstance) {
            this.policyInstance = policyInstance;
        }

        public Builder definition(PolicyMetadata policyMetadata) {
            this.policyMetadata = policyMetadata;
            return this;
        }

        public PolicyImpl build() {
            return new PolicyImpl(policyInstance)
                    .definition(policyMetadata);
        }
    }
}
