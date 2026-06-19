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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.gravitee.gateway.services.sync.process.common.model.Deployable;
import java.util.Set;

/**
 * A deployable targeted at a set of PDP scopes. Lets {@link AbstractAuthzReactorSynchronizer} drive
 * the shared scope-placement / eviction logic uniformly across entities and policies.
 */
public interface AuthzScopedDeployable extends Deployable {
    String environmentId();

    Set<String> targetPdpIds();

    AuthzScopedDeployable targetPdpIds(Set<String> targetPdpIds);

    AuthzScopedDeployable removedTargetPdpIds(Set<String> removedTargetPdpIds);
}
