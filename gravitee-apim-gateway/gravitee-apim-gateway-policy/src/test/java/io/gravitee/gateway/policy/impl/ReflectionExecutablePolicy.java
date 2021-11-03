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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import java.lang.reflect.Method;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReflectionExecutablePolicy implements Policy {

    private final String id;
    private final Object policy;
    private final Method headMethod, streamMethod;

    ReflectionExecutablePolicy(String id, Object policy, Method headMethod, Method streamMethod) {
        this.id = id;
        this.policy = policy;
        this.headMethod = headMethod;
        this.streamMethod = streamMethod;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void execute(PolicyChain chain, ExecutionContext context) throws PolicyException {
        invoke(headMethod, chain, context, context.request(), context.response());
    }

    @Override
    public ReadWriteStream<Buffer> stream(PolicyChain chain, ExecutionContext context) throws PolicyException {
        Object stream = invoke(streamMethod, chain, context, context.request(), context.response());
        return (stream != null) ? (ReadWriteStream<Buffer>) stream : null;
    }

    @Override
    public boolean isStreamable() {
        return streamMethod != null;
    }

    @Override
    public boolean isRunnable() {
        return headMethod != null;
    }

    private Object invoke(Method invokedMethod, Object... args) throws PolicyException {
        if (invokedMethod != null) {
            Class<?>[] parametersType = invokedMethod.getParameterTypes();
            Object[] parameters = new Object[parametersType.length];

            int idx = 0;

            // Map parameters according to parameter's type
            for (Class<?> paramType : parametersType) {
                parameters[idx++] = getParameterAssignableTo(paramType, args);
            }

            try {
                return invokedMethod.invoke(policy, parameters);
            } catch (Exception ex) {
                throw new PolicyException(ex);
            }
        }

        return null;
    }

    private <T> T getParameterAssignableTo(Class<T> paramType, Object... args) {
        for (Object arg : args) {
            if (paramType.isAssignableFrom(arg.getClass())) {
                return (T) arg;
            }
        }

        return null;
    }
}
