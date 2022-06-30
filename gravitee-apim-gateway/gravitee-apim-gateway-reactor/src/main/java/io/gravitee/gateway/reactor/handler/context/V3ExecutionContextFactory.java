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
package io.gravitee.gateway.reactor.handler.context;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.ExecutionContext;

/**
 * A factory of {@link ExecutionContext}. A single instance is created on per {@link io.gravitee.gateway.reactor.Reactable}
 * basis because {@link TemplateVariableProvider} providers list is containing provider specific to the reactable.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface V3ExecutionContextFactory {
    /**
     * Create a new {@link ExecutionContext} for each of the incoming request to the gateway.
     *
     * @param wrapped
     * @return
     */
    ExecutionContext create(ExecutionContext wrapped);
}
