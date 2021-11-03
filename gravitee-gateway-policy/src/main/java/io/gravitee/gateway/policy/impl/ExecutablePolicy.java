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
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import jdk.dynalink.linker.support.Lookup;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExecutablePolicy implements Policy {

    private final String id;
    private final Object policy;
    private final MethodHandle headMethodHandle;
    private final MethodHandle streamMethodHandle;

    ExecutablePolicy(String id, Object policy, Method headMethod, Method streamMethod) {
        this.id = id;
        this.policy = policy;

        // Optimize the reflection call by relying on MethodHandle and reordering of arguments.
        headMethodHandle = toMethodHandle(headMethod);
        streamMethodHandle = toMethodHandle(streamMethod);
    }

    private MethodHandle toMethodHandle(Method method) {
        final MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        if (method != null) {
            final MethodType invokedMethodType = MethodType.methodType(
                method.getReturnType(),
                method.getDeclaringClass(),
                PolicyChain.class,
                ExecutionContext.class,
                Request.class,
                Response.class
            );

            final MethodHandle originalHeadMethodHandle = Lookup.unreflect(lookup, method);
            final MethodType originalMethodType = originalHeadMethodHandle.type();
            final int[] reorder = getReorder(originalMethodType, invokedMethodType);

            return MethodHandles.permuteArguments(originalHeadMethodHandle, invokedMethodType, reorder);
        }

        return null;
    }

    private int[] getReorder(MethodType originalMethodType, MethodType methodType) {
        final int[] reorder = new int[originalMethodType.parameterCount()];

        for (int i = 0; i < originalMethodType.parameterCount(); i++) {
            final Class<?> originalParameterType = originalMethodType.parameterType(i);

            boolean found = false;
            for (int j = 0; j < methodType.parameterCount(); j++) {
                final Class<?> parameterType = methodType.parameterType(j);
                if (parameterType.isAssignableFrom(originalParameterType)) {
                    reorder[i] = j;
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Invalid policy parameters");
            }
        }
        return reorder;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void execute(PolicyChain chain, ExecutionContext context) throws PolicyException {
        try {
            headMethodHandle.invoke(policy, chain, context, context.request(), context.response());
        } catch (Throwable ex) {
            throw new PolicyException(ex);
        }
    }

    @Override
    public ReadWriteStream<Buffer> stream(PolicyChain chain, ExecutionContext context) throws PolicyException {
        try {
            Object stream = streamMethodHandle.invoke(policy, chain, context, context.request(), context.response());
            return (stream != null) ? (ReadWriteStream<Buffer>) stream : null;
        } catch (Throwable ex) {
            throw new PolicyException(ex);
        }
    }

    @Override
    public boolean isStreamable() {
        return streamMethodHandle != null;
    }

    @Override
    public boolean isRunnable() {
        return headMethodHandle != null;
    }
}
