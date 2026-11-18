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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;

/**
 * Implemented by an execution context that decorates another one, so that code which needs the concrete context
 * behind the decoration (for instance the debug hooks) can reach it.
 */
public interface WrappedExecutionContext {
    /**
     * @return the execution context this one decorates
     */
    HttpExecutionContext unwrap();
}
