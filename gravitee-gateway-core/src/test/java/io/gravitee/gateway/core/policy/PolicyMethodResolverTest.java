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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.policy.PolicyChain;
import io.gravitee.gateway.api.policy.annotations.OnRequest;
import io.gravitee.gateway.api.policy.annotations.OnResponse;
import io.gravitee.gateway.core.policy.impl.PolicyMethodResolverImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyMethodResolverTest {

    private final PolicyMethodResolver policyMethodResolver = new PolicyMethodResolverImpl();

    @Test
    public void resolvePolicyMethods_empty() {
        Map methods = policyMethodResolver.resolvePolicyMethods(DummyPolicy01.class);
        Assert.assertTrue(methods.isEmpty());
    }

    @Test
    public void resolvePolicyMethods_onlyTwoMethods() {
        Map methods = policyMethodResolver.resolvePolicyMethods(DummyPolicy02.class);
        Assert.assertEquals(2, methods.size());
    }

    @Test
    public void resolvePolicyMethods_moreThanTwoMethods() {
        Map methods = policyMethodResolver.resolvePolicyMethods(DummyPolicy03.class);
        Assert.assertEquals(2, methods.size());
    }

    class DummyPolicy01 {

    }

    class DummyPolicy02 {

        @OnRequest
        public void onRequest(PolicyChain handler, Request request, Response response) {
            // Do nothing
        }

        @OnResponse
        public void onResponse(Request request, Response response, PolicyChain handler) {
            // Do nothing
        }
    }

    class DummyPolicy03 {

        @OnRequest
        public void onRequest(PolicyChain handler, Request request, Response response) {
            // Do nothing
        }

        @OnRequest
        public void onRequest2(PolicyChain handler, Request request, Response response) {
            // Do nothing
        }

        @OnResponse
        public void onResponse(Request request, Response response, PolicyChain handler) {
            // Do nothing
        }
    }
}
