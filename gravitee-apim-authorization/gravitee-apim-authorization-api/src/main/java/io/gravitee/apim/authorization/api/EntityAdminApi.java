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
package io.gravitee.apim.authorization.api;

import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.service.CascadeResult;
import io.gravitee.apim.authorization.service.CreateOrReplaceEntityCommand;
import io.gravitee.apim.authorization.service.EntityFilter;
import io.gravitee.apim.authorization.service.UpdateEntityCommand;
import io.gravitee.apim.authorization.service.UpsertResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EntityAdminApi {
    UpsertResult upsert(AuthzCallerContext caller, CreateOrReplaceEntityCommand command);

    Entity update(AuthzCallerContext caller, String entityId, UpdateEntityCommand command);

    Optional<Entity> findByEntityId(String environmentId, String entityId);

    List<Entity> find(String environmentId, EntityFilter filter);

    Set<String> findApiAliases(String environmentId, String apiId);

    CascadeResult delete(AuthzCallerContext caller, String entityId);
}
