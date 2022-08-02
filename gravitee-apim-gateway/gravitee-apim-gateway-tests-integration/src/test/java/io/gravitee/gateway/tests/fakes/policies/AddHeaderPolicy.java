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
package io.gravitee.gateway.tests.fakes.policies;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;
import io.reactivex.Completable;

/**
 * Policy that add "{@link AddHeaderPolicy#HEADER_NAME}" on:
 * - request with value {@link AddHeaderPolicy#REQUEST_HEADER}
 * - response with value {@link AddHeaderPolicy#RESPONSE_HEADER}
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AddHeaderPolicy implements Policy {

    public static final String HEADER_NAME = "X-Header-Test";
    public static final String REQUEST_HEADER = "response-header";
    public static final String RESPONSE_HEADER = "request-header";

    @OnRequest
    public void onRequest(final Request request, final Response response, final PolicyChain policyChain) {
        request.headers().add(HEADER_NAME, REQUEST_HEADER);
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(final Request request, final Response response, final PolicyChain policyChain) {
        response.headers().add(HEADER_NAME, RESPONSE_HEADER);
        policyChain.doNext(request, response);
    }

    @Override
    public String id() {
        return "add-header";
    }

    @Override
    public Completable onRequest(RequestExecutionContext ctx) {
        return Completable.fromCallable(() -> ctx.request().headers().add(HEADER_NAME, REQUEST_HEADER));
    }

    @Override
    public Completable onResponse(RequestExecutionContext ctx) {
        return Completable.fromCallable(() -> ctx.response().headers().add(HEADER_NAME, RESPONSE_HEADER));
    }
}
