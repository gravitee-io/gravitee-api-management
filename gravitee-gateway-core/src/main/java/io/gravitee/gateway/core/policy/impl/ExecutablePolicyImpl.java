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

import io.gravitee.gateway.core.policy.ExecutablePolicy;
import io.gravitee.gateway.core.policy.PolicyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ExecutablePolicyImpl implements ExecutablePolicy {

    private final Logger LOGGER = LoggerFactory.getLogger(ExecutablePolicyImpl.class);

    private final Object policyInst;

    private final PolicyDefinition policyDefinition;

    public ExecutablePolicyImpl(PolicyDefinition policyDefinition, Object policyInst) {
        this.policyDefinition = policyDefinition;
        this.policyInst = policyInst;
    }

    @Override
    public void onRequest(Object ... args) throws Exception {
        LOGGER.debug("Calling onRequest method on policy {}", policyDefinition.policy().getName());

        Method reqMethod = policyDefinition.onRequestMethod();
        invoke(reqMethod, args);
    }

    @Override
    public void onResponse(Object ... args) throws Exception {
        LOGGER.debug("Calling onResponse method on policy {}", policyDefinition.policy().getName());

        Method resMethod = policyDefinition.onResponseMethod();
        invoke(resMethod, args);
    }

    private void invoke(Method invokeMethod, Object ... args) throws Exception {
        if (invokeMethod != null) {
            Class<?>[] parametersType = invokeMethod.getParameterTypes();
            Object[] parameters = new Object[parametersType.length];
            int idx = 0;

            // Map correctly parameters according to parameter's type
            for(Class<?> paramType : parametersType) {
                //TODO: map data according to parameter type
                parameters[idx] = args[idx - 1];
                /*
                if (paramType.equals(PolicyChain.class)) {
                    parameters[idx++] = policyChain;
                } else if (paramType.equals(Request.class)) {
                    parameters[idx++] = request;
                } else if (paramType.equals(Response.class)) {
                    parameters[idx++] = response;
                }
                */
            }

            invokeMethod.invoke(policyInst, parameters);
        }
    }

    @Override
    public Object getPolicy() {
        return policyInst;
    }

    @Override
    public PolicyDefinition getPolicyDefinition() {
        return policyDefinition;
    }
}
