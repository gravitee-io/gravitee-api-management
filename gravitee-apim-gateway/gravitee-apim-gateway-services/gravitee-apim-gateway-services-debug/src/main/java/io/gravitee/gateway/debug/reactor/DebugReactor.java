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
package io.gravitee.gateway.debug.reactor;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.impl.DefaultReactor;
import io.gravitee.gateway.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;

public class DebugReactor extends DefaultReactor {

    public DebugReactor(
        EventManager eventManager,
        EntrypointResolver entrypointResolver,
        ReactorHandlerRegistry reactorHandlerRegistry,
        GatewayConfiguration gatewayConfiguration,
        RequestProcessorChainFactory requestProcessorChainFactory,
        ResponseProcessorChainFactory responseProcessorChainFactory,
        NotFoundProcessorChainFactory notFoundProcessorChainFactory
    ) {
        super(
            eventManager,
            entrypointResolver,
            reactorHandlerRegistry,
            gatewayConfiguration,
            requestProcessorChainFactory,
            responseProcessorChainFactory,
            notFoundProcessorChainFactory
        );
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> reactorEvent) {
        // Ignore events
    }
}
