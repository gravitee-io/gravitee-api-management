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
package io.gravitee.gateway.debug.policy;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyPolicy {

    @OnRequest
    public void onRequest(PolicyChain chain, Request request, Response response) {
        // Do nothing
        chain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(PolicyChain chain, Request request, Response response) {
        // Do nothing
        chain.doNext(request, response);
    }

    @OnRequestContent
    public void onRequestContent(PolicyChain chain, Request request, Response response) {
        // Do nothing
    }

    @OnResponseContent
    public void onResponseContent(PolicyChain chain, Request request, Response response) {
        // Do nothing
    }
}
