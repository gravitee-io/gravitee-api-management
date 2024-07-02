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
package io.gravitee.gateway.services.sync.process.repository.fetcher;

import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrganizationIdsFetcher {

    private final EnvironmentRepository environmentRepository;
    private final GatewayConfiguration configuration;

    public Maybe<Set<String>> fetch(final Set<String> environments) {
        if (!configuration.useLegacyEnvironmentHrids() && environments != null) {
            return Maybe.defer(() -> {
                Set<String> organizationIdsByEnvironments = environmentRepository.findOrganizationIdsByEnvironments(environments);
                if (organizationIdsByEnvironments != null) {
                    return Maybe.just(organizationIdsByEnvironments);
                } else {
                    return Maybe.empty();
                }
            });
        }
        return Maybe.empty();
    }
}
