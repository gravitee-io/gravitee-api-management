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

import io.gravitee.gateway.core.policy.Policy;
import io.gravitee.policy.api.PolicyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyImpl implements Policy {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyImpl.class);

    private final PolicyContext policyContext;
    private final Object policyInst;
    private final Method onRequestMethod, onResponseMethod;

    public PolicyImpl(Object policyInst, PolicyContext policyContext, Method onRequestMethod, Method onResponseMethod) {
        this.policyInst = policyInst;
        this.policyContext = policyContext;
        this.onRequestMethod = onRequestMethod;
        this.onResponseMethod = onResponseMethod;
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

    private void invoke(Method invokedMethod, Object ... args) throws Exception {
        LOGGER.debug("Calling {} method on policy {}", invokedMethod.getName(), policyInst.getClass().getName());

        Class<?>[] parametersType = invokedMethod.getParameterTypes();
        Object[] parameters = new Object[parametersType.length];

        int idx = 0;

        // Map parameters according to parameter's type
        for(Class<?> paramType : parametersType) {
            parameters[idx++] = getParameterAssignableTo(paramType, args);
        }

        invokedMethod.invoke(policyInst, parameters);
    }

    private Object getParameterAssignableTo(Class<?> paramType, Object ... args) {
        for(Object arg: args) {
            if (paramType.isAssignableFrom(arg.getClass())) {
                return arg;
            }
        }

        if (paramType.isAssignableFrom(PolicyContext.class)) {
            return policyContext;
        }

        return null;
    }
}
