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
package io.gravitee.apim.integration.tests.fake;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnResponse;
import io.reactivex.rxjava3.core.Completable;
import java.util.Optional;

public class AttributesToHeaderPolicy implements Policy {

    @OnResponse
    public void onResponse(final Request request, final Response response, ExecutionContext ctx, final PolicyChain policyChain) {
        ctx
            .getAttributeNames()
            .asIterator()
            .forEachRemaining(attribute ->
                response.headers().add(attribute, Optional.ofNullable(ctx.getAttribute(attribute)).map(Object::toString).orElse("null"))
            );
        policyChain.doNext(request, response);
    }

    @Override
    public String id() {
        return "attributes-to-header";
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return Completable.fromRunnable(() ->
            ctx
                .getAttributeNames()
                .forEach(attribute ->
                    ctx
                        .response()
                        .headers()
                        .add(attribute, Optional.ofNullable(ctx.getAttribute(attribute)).map(Object::toString).orElse("null"))
                )
        );
    }
}
