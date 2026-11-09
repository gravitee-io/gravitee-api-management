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
import java.util.Date;

/**
 * Decides what a control plane event means for an API or dictionary that has a local version:
 * an event newer than the version first edited replaces the local version, otherwise the local version stays.
 * An empty result means the event must be ignored.
 *
 * @author GraviteeSource Team
 */
public interface LocalVersionResolver {
    Maybe<ReactableApi<?>> resolveDeployment(ReactableApi<?> api);

    Maybe<String> resolveUndeployment(String apiId, Date eventCreatedAt);

    Maybe<Dictionary> resolveDeployment(Dictionary dictionary);

    Maybe<Dictionary> resolveUndeployment(Dictionary dictionary, Date eventCreatedAt);
}
