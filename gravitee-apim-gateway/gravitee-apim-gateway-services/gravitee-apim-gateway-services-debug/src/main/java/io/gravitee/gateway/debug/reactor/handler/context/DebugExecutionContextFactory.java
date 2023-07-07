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
package io.gravitee.gateway.debug.reactor.handler.context;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.reactor.handler.context.V3ExecutionContextFactory;

/**
 * The ExecutionContextFactory for debug mode.
 *
 * {@inheritDoc}
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugExecutionContextFactory implements V3ExecutionContextFactory {

    private V3ExecutionContextFactory delegate;

    public DebugExecutionContextFactory(V3ExecutionContextFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public ExecutionContext create(ExecutionContext executionContext) {
        return new DebugExecutionContext(delegate.create(executionContext));
    }
}
