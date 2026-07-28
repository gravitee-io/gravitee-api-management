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
 * Test policy that assigns a context attribute during the request phase.
 *
 * @author GraviteeSource Team
 */
public class SetAttributePolicy implements Policy {

    public static final String ATTRIBUTE_NAME = "foo";
    public static final String ATTRIBUTE_VALUE = "bar";

    @Override
    public String id() {
        return "set-attribute";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return Completable.fromRunnable(() -> ctx.setAttribute(ATTRIBUTE_NAME, ATTRIBUTE_VALUE));
    }
}
