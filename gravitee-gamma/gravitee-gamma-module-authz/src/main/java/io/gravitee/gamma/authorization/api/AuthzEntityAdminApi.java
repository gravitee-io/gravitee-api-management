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

import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.service.AuthzCascadeResult;
import io.gravitee.gamma.authorization.service.AuthzEntityFilter;
import io.gravitee.gamma.authorization.service.AuthzUpsertResult;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import io.gravitee.gamma.authorization.service.UpdateAuthzEntityCommand;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AuthzEntityAdminApi {
    AuthzUpsertResult upsert(AuthzCallerContext caller, CreateOrReplaceAuthzEntityCommand command);

    List<AuthzUpsertResult> bulkUpsert(AuthzCallerContext caller, List<CreateOrReplaceAuthzEntityCommand> commands);

    AuthzEntity update(AuthzCallerContext caller, String entityId, UpdateAuthzEntityCommand command);

    Optional<AuthzEntity> findByEntityId(String environmentId, String entityId);

    List<AuthzEntity> find(String environmentId, AuthzEntityFilter filter);

    PagedResult<AuthzEntity> findPage(String environmentId, AuthzEntityFilter filter, Pageable pageable);

    Set<String> findApiAliases(String environmentId, String apiId);

    AuthzCascadeResult delete(AuthzCallerContext caller, String entityId);
}
