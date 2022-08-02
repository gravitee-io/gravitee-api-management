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
package io.gravitee.gateway.jupiter.reactor.handler.context;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableMessageExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableMessageRequest;
import io.gravitee.gateway.jupiter.core.context.MutableMessageResponse;

/**
 * Default implementation of {@link MessageExecutionContext} to use when handling async api requests.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultMessageExecutionContext
    extends AbstractExecutionContext<MutableMessageRequest, MutableMessageResponse>
    implements MutableMessageExecutionContext {

    public DefaultMessageExecutionContext(final MutableMessageRequest request, final MutableMessageResponse response) {
        super(request, response);
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        //FIXME should be implemented to properly handle message execution context
        throw new UnsupportedOperationException("Not Implemented yet");
    }
}
