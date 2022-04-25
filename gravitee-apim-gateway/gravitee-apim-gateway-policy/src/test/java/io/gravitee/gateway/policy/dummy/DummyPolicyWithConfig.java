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
package io.gravitee.gateway.policy.dummy;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyPolicyWithConfig {

    private DummyPolicyConfiguration dummyPolicyConfiguration;

    public DummyPolicyWithConfig(final DummyPolicyConfiguration dummyPolicyConfiguration) {
        this.dummyPolicyConfiguration = dummyPolicyConfiguration;
    }

    public DummyPolicyConfiguration getDummyPolicyConfiguration() {
        return dummyPolicyConfiguration;
    }

    @OnRequest
    public void onRequest(PolicyChain chain, Request request, Response response) {
        // Do nothing
    }

    @OnResponse
    public void onResponse(Request chain, Response response, PolicyChain handler) {
        // Do nothing
    }

    @OnRequestContent
    public void onRequestContent(PolicyChain chain, Request request, Response response) {
        // Do nothing
    }

    @OnResponseContent
    public void onResponseContent(Request chain, Response response, PolicyChain handler) {
        // Do nothing
    }
}
