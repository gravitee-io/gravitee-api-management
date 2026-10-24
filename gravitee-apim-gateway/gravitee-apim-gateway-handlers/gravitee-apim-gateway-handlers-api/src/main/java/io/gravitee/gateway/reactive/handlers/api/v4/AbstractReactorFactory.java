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
package io.gravitee.gateway.reactive.handlers.api.v4;

import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.node.api.configuration.Configuration;
import org.springframework.context.ApplicationContext;

/**
 * Former name of {@link AbstractApiReactorFactory}, kept so that the reactor plugins released against it still load.
 *
 * @deprecated extend {@link AbstractApiReactorFactory} instead.
 */
@Deprecated(forRemoval = true)
public abstract class AbstractReactorFactory<T extends ReactableApi<? extends AbstractApi>> extends AbstractApiReactorFactory<T> {

    protected AbstractReactorFactory(
        ApplicationContext applicationContext,
        PolicyFactoryManager policyFactoryManager,
        Configuration configuration,
        DictionaryManager dictionaryManager
    ) {
        super(applicationContext, policyFactoryManager, configuration, dictionaryManager);
    }
}
