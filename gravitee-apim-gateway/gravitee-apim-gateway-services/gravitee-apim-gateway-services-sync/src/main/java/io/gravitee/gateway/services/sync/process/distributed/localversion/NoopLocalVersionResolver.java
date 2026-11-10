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
package io.gravitee.gateway.services.sync.process.distributed.localversion;

import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.reactor.ReactableApi;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Date;

/**
 * @author GraviteeSource Team
 */
public class NoopLocalVersionResolver implements LocalVersionResolver {

    @Override
    public Maybe<ReactableApi<?>> resolveDeployment(final ReactableApi<?> api) {
        return Maybe.just(api);
    }

    @Override
    public Maybe<String> resolveUndeployment(final String apiId, final Date eventCreatedAt) {
        return Maybe.just(apiId);
    }

    @Override
    public Maybe<Dictionary> resolveDeployment(final Dictionary dictionary) {
        return Maybe.just(dictionary);
    }

    @Override
    public Maybe<Dictionary> resolveUndeployment(final Dictionary dictionary, final Date eventCreatedAt) {
        return Maybe.just(dictionary);
    }

    @Override
    public SubscriptionLocalVersions subscriptionLocalVersions() {
        return Single::just;
    }
}
