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

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.message.MessageFlow;
import io.gravitee.gateway.reactive.reactor.handler.message.DefaultMessageFlow;
import io.reactivex.Flowable;
import java.util.List;

/**
 * Default implementation of {@link MessageExecutionContext} to use when handling async api requests.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultMessageExecutionContext extends AbstractExecutionContext implements MessageExecutionContext {

    private final MessageFlow incomingMessageFlow;
    private final MessageFlow outgoingMessageFlow;

    public DefaultMessageExecutionContext(
        Request request,
        Response response,
        ComponentProvider componentProvider,
        List<TemplateVariableProvider> templateVariableProviders
    ) {
        super(request, response, componentProvider, templateVariableProviders);
        incomingMessageFlow = new DefaultMessageFlow(Flowable.empty());
        outgoingMessageFlow = new DefaultMessageFlow(Flowable.empty());
    }

    @Override
    public MessageFlow incomingMessageFlow() {
        return incomingMessageFlow;
    }

    @Override
    public MessageFlow outgoingMessageFlow() {
        return outgoingMessageFlow;
    }
}
