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
 * Policy that add a header for each Path Parameter:
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathParamToHeaderPolicy implements Policy {

    public static final String X_PATH_PARAM = "X-PathParam-";

    @OnRequest
    public void onRequest(final Request request, final Response response, final PolicyChain policyChain) {
        if (request.pathParameters() != null) {
            request.pathParameters().forEach((key, value) -> request.headers().add(X_PATH_PARAM + key, value.get(0)));
        }
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(final Request request, final Response response, final PolicyChain policyChain) {
        if (request.pathParameters() != null) {
            request.pathParameters().forEach((key, value) -> response.headers().add(X_PATH_PARAM + key, value.get(0)));
        }
        policyChain.doNext(request, response);
    }

    @Override
    public String id() {
        return "path-param-to-header";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            if (ctx.request().pathParameters() != null) {
                ctx
                    .request()
                    .pathParameters()
                    .forEach((key, value) -> ctx.request().headers().add(X_PATH_PARAM + key, value.get(0)));
            }
        });
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            if (ctx.request().pathParameters() != null) {
                ctx
                    .request()
                    .pathParameters()
                    .forEach((key, value) -> ctx.response().headers().add(X_PATH_PARAM + key, value.get(0)));
            }
        });
    }
}
