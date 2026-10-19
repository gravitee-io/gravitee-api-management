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
package io.gravitee.definition.model.cluster;

import io.reactivex.rxjava3.core.Single;

/**
 * Evaluates the secret references of a registered Kafka cluster connection's security on the gateway.
 *
 * <p>The gateway discovers the secret references of a cluster when it deploys it, as it does for APIs and shared policy
 * groups. The security the {@link ClusterRegistry} holds then carries expressions, not credentials: every component
 * building a Kafka client from a cluster connection resolves it here first, so both API types read the same values.</p>
 *
 * <p>Only the gateway's secrets are available: a cluster serves many APIs, so request or API expressions
 * ({@code #request}, {@code #context}, {@code #api}…) fail to evaluate. Values without an expression are returned as
 * is.</p>
 */
public interface ClusterSecurityResolver {
    /**
     * @return a copy of {@code security} whose expressions are evaluated; fails with
     *     {@link ClusterSecurityResolutionException} naming the field that could not be resolved, never its value.
     */
    Single<SecurityConfig> resolve(SecurityConfig security);
}
