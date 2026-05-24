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

import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.service.AuthzPolicyFilter;
import io.gravitee.gamma.authorization.service.CreateAuthzPolicyCommand;
import io.gravitee.gamma.authorization.service.UpdateAuthzPolicyCommand;
import java.util.List;
import java.util.Optional;

public interface AuthzPolicyAdminApi {
    AuthzPolicy create(AuthzCallerContext caller, CreateAuthzPolicyCommand command);

    AuthzPolicy update(AuthzCallerContext caller, String id, UpdateAuthzPolicyCommand command);

    AuthzPolicy deploy(AuthzCallerContext caller, String id);

    AuthzPolicy disable(AuthzCallerContext caller, String id);

    Optional<AuthzPolicy> findById(String environmentId, String id);

    List<AuthzPolicy> findAll(String environmentId);

    List<AuthzPolicy> findByKind(String environmentId, AuthzPolicyKind kind);

    List<AuthzPolicy> findByEntityId(String environmentId, String entityId);

    PagedResult<AuthzPolicy> findPage(String environmentId, AuthzPolicyFilter filter, Pageable pageable);

    boolean delete(AuthzCallerContext caller, String id);
}
