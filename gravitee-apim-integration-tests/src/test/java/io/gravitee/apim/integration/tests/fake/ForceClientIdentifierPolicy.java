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

import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.reactivex.rxjava3.core.Completable;

/**
 * This policy forces the client identifier with 'custom' value.
 * It is specifically useful when running parallel tests with webhook when there is no real http request and it is not possible to specify a particular request header to override it.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ForceClientIdentifierPolicy implements Policy {

    @Override
    public String id() {
        return "force-client-identifier";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        ((MutableExecutionContext) ctx).request().clientIdentifier("custom");
        return Policy.super.onRequest(ctx);
    }
}
