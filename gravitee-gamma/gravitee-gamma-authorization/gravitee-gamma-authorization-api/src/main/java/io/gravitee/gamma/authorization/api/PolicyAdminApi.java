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
package io.gravitee.gamma.authorization.api;

import io.gravitee.gamma.authorization.domain.Policy;
import io.gravitee.gamma.authorization.domain.PolicyKind;
import io.gravitee.gamma.authorization.service.CreatePolicyCommand;
import io.gravitee.gamma.authorization.service.PolicyFilter;
import io.gravitee.gamma.authorization.service.UpdatePolicyCommand;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.List;
import java.util.Optional;

public interface PolicyAdminApi {
    Policy create(AuthzCallerContext caller, CreatePolicyCommand command);

    Policy update(AuthzCallerContext caller, String id, UpdatePolicyCommand command);

    Policy deploy(AuthzCallerContext caller, String id);

    Policy disable(AuthzCallerContext caller, String id);

    Optional<Policy> findById(String environmentId, String id);

    List<Policy> findAll(String environmentId);

    List<Policy> findByKind(String environmentId, PolicyKind kind);

    List<Policy> findByEntityId(String environmentId, String entityId);

    /**
     * Paginated lookup of policies matching {@code filter}. Use this for
     * any UI-driven listing; the unpaged finders above stay for whole-env
     * iteration (gateway sync warm-up, schema invalidation, etc).
     */
    PagedResult<Policy> findPage(String environmentId, PolicyFilter filter, Pageable pageable);

    boolean delete(AuthzCallerContext caller, String id);
}
