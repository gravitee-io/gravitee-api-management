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
package io.gravitee.gateway.core.invoker;

import io.gravitee.gateway.reactive.api.context.ContextAttributes;

/**
 * Context attributes shared between the legacy (v3) gateway engine components.
 */
public final class V3InvokerAttributes {

    /**
     * Internal attribute holding a {@link Runnable} that cancels the upstream call of a v3 exchange.
     * It is published as soon as an upstream call is in flight and invoked when the client goes away before the exchange ends.
     */
    public static final String ATTR_INTERNAL_V3_CANCEL_UPSTREAM = ContextAttributes.ATTR_PREFIX + "v3-cancel-upstream";

    private V3InvokerAttributes() {}
}
