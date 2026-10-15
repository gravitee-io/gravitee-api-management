/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;
import io.reactivex.rxjava3.core.Completable;

/**
 * Reports, as response headers, what a policy sees of the request path.
 *
 * <p>Implements both the reactive and the legacy policy API so the same assertions can be made
 * whichever engine executes the API. This is the only way to observe from the outside whether a
 * policy reasons on the path the gateway resolved or on the one it received — the difference a
 * path-scoped policy stands or falls on.
 *
 * @author GraviteeSource Team
 */
public class PathToHeaderPolicy implements Policy {

    public static final String X_PATH = "X-Observed-Path";
    public static final String X_PATH_INFO = "X-Observed-PathInfo";
    public static final String X_URI = "X-Observed-Uri";

    @OnRequest
    public void onRequest(final Request request, final Response response, final PolicyChain policyChain) {
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(final Request request, final Response response, final PolicyChain policyChain) {
        response.headers().add(X_PATH, request.path());
        response.headers().add(X_PATH_INFO, request.pathInfo());
        response.headers().add(X_URI, request.uri());
        policyChain.doNext(request, response);
    }

    @Override
    public String id() {
        return "path-to-header";
    }

    @Override
    public Completable onResponse(final HttpExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            ctx.response().headers().add(X_PATH, ctx.request().path());
            ctx.response().headers().add(X_PATH_INFO, ctx.request().pathInfo());
            ctx.response().headers().add(X_URI, ctx.request().uri());
        });
    }
}
