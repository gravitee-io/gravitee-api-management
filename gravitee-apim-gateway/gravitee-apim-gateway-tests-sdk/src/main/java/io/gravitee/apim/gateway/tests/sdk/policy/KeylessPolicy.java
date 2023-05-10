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
package io.gravitee.apim.gateway.tests.sdk.policy;

import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_APPLICATION;
import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_SUBSCRIPTION_ID;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.SecurityPolicy;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeylessPolicy implements SecurityPolicy {

    @OnRequest
    public void onRequest(Request request, Response response, PolicyChain policyChain) {
        policyChain.doNext(request, response);
    }

    @Override
    public String id() {
        return "keyless";
    }

    @Override
    public int order() {
        return 1000;
    }

    @Override
    public boolean requireSubscription() {
        return false;
    }

    @Override
    public Single<Boolean> support(RequestExecutionContext ctx) {
        return Single.just(Boolean.TRUE);
    }

    @Override
    public Completable onRequest(RequestExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            ctx.setAttribute(ATTR_APPLICATION, "1");
            ctx.setAttribute(ATTR_SUBSCRIPTION_ID, ctx.request().remoteAddress());
        });
    }
}
