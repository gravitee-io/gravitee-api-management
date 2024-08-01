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
package io.gravitee.gateway.handlers.sharedpolicygroup.reactor;

import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SharedPolicyGroupReactorFactoryManager {

    private final List<SharedPolicyGroupReactorFactory> sharedPolicyGroupReactorFactories;

    // Inject embedded factories via spring configuration
    public SharedPolicyGroupReactorFactoryManager(final List<SharedPolicyGroupReactorFactory> sharedPolicyGroupReactorFactories) {
        this.sharedPolicyGroupReactorFactories = new ArrayList<>(sharedPolicyGroupReactorFactories);
    }

    /**
     * Registers a {@link SharedPolicyGroupReactorFactory}. Useful for Reactor plugins.
     * @param sharedPolicyGroupReactorFactory to register
     */
    public void register(SharedPolicyGroupReactorFactory sharedPolicyGroupReactorFactory) {
        sharedPolicyGroupReactorFactories.add(sharedPolicyGroupReactorFactory);
    }

    public SharedPolicyGroupReactor create(final ReactableSharedPolicyGroup reactable) {
        return sharedPolicyGroupReactorFactories
            .stream()
            .filter(reactorFactory -> reactorFactory.canCreate(reactable))
            .map(reactorFactory -> reactorFactory.create(reactable))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No reactor found for reactable: " + reactable.getId()));
    }
}
