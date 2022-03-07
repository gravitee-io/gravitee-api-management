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

import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.policy.DummyPolicy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.plugin.policy.internal.PolicyMethodResolver;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
public class ExecutablePolicyBenchmark {

    DummyPolicy policy;
    PolicyManifest policyManifest;
    Method requestMethod;
    ExecutablePolicy executablePolicy;
    ReflectionExecutablePolicy reflectionExecutablePolicy;
    SimpleExecutionContext executionContext;
    OrderedPolicyChain policyChain;

    @Setup
    public void setup() {
        policy = new DummyPolicy();

        policyManifest =
            new PolicyManifestBuilder()
                .setPolicy(DummyPolicy.class)
                .setId("dummy")
                .setMethods(new PolicyMethodResolver().resolve(DummyPolicy.class))
                .build();

        requestMethod = policyManifest.method(OnRequest.class);
        executablePolicy = new ExecutablePolicy("dummy", policy, requestMethod, policyManifest.method(OnRequestContent.class));

        reflectionExecutablePolicy =
            new ReflectionExecutablePolicy(
                "dummy",
                policy,
                policyManifest.method(OnRequest.class),
                policyManifest.method(OnRequestContent.class)
            );

        executionContext = new SimpleExecutionContext(new SimpleRequest(), new SimpleResponse());
        policyChain = OrderedPolicyChain.create(Collections.emptyList(), executionContext);
    }

    @Benchmark
    public void benchDirect() throws PolicyException {
        policy.onRequest(policyChain, executionContext.request(), executionContext.response());
    }

    @Benchmark
    public void benchReflection() throws PolicyException {
        reflectionExecutablePolicy.execute(policyChain, executionContext);
    }

    @Benchmark
    public void benchReflectionDirectCall() throws Exception {
        requestMethod.invoke(policy, policyChain, executionContext.request(), executionContext.response());
    }

    @Benchmark
    public void benchMethodHandle() throws PolicyException {
        executablePolicy.execute(policyChain, executionContext);
    }
}
