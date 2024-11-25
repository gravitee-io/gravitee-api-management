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
package io.gravitee.apim.gateway.tests.sdk.connector.fakes;

import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnector;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PersistentMockEndpointConnector extends MockEndpointConnector {

    private final MessageStorage messageStorage;

    public PersistentMockEndpointConnector(MockEndpointConnectorConfiguration configuration, MessageStorage messageStorage) {
        super(configuration);
        this.messageStorage = messageStorage;
    }

    @Override
    public Completable publish(HttpExecutionContext ctx) {
        return Completable
            .defer(() ->
                ctx
                    .request()
                    .onMessage(message -> {
                        messageStorage.subject().onNext(message);
                        return Maybe.just(message);
                    })
            )
            .andThen(super.publish(ctx));
    }
}
