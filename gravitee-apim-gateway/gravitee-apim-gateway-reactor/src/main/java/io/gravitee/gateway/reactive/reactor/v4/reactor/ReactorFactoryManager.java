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
package io.gravitee.gateway.reactive.reactor.v4.reactor;

import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ReactorFactoryManager {

    private final List<ReactorFactory> reactorFactories;

    // Inject embedded factories via spring configuration
    public ReactorFactoryManager(final List<ReactorFactory> reactorFactories) {
        this.reactorFactories = reactorFactories;
    }

    /**
     * Registers a {@link ReactorFactory}. Useful for Reactor plugins.
     * @param reactorFactory to register
     */
    public void register(ReactorFactory<?> reactorFactory) {
        reactorFactories.add(reactorFactory);
    }

    public List<ReactorHandler> create(final Reactable reactable) {
        if (reactable != null) {
            return reactorFactories
                .stream()
                .filter(reactorFactory -> reactorFactory.support(reactable.getClass()))
                .filter(reactorFactory -> reactorFactory.canCreate(reactable))
                .map(reactorFactory -> reactorFactory.create(reactable))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
