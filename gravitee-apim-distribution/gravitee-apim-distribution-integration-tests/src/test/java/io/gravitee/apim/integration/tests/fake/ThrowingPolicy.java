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
package io.gravitee.apim.integration.tests.fake;

import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.reactivex.rxjava3.core.Completable;

/**
 * A policy that is just throwing a RuntimeException
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ThrowingPolicy implements Policy {

    @Override
    public String id() {
        return "throwing-policy";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return Completable.error(new RuntimeException());
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return Completable.error(new RuntimeException());
    }
}
