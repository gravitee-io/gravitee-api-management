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
package io.gravitee.gateway.reactive.reactor.handler.context;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.sync.SyncRequest;
import io.gravitee.gateway.reactive.api.context.sync.SyncResponse;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncExecutionContext
    extends AbstractExecutionContext<SyncRequest, SyncResponse>
    implements io.gravitee.gateway.reactive.api.context.sync.SyncExecutionContext {

    public SyncExecutionContext(SyncRequest request, SyncResponse response, ComponentProvider componentProvider) {
        super(request, response, componentProvider);
    }
}
