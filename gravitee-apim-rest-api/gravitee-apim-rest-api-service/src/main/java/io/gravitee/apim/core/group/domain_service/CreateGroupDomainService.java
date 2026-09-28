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
package io.gravitee.apim.core.group.domain_service;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.Group;

/**
 * Creates an empty environment group with V2 {@code GroupService.create} semantics:
 * unique name per environment and {@code GROUP_CREATED} audit.
 */
public interface CreateGroupDomainService {
    /**
     * Result of a group creation attempt, indicating whether the group was newly created
     * or an existing group was reused (due to name collision or concurrent create race).
     */
    record CreateResult(Group group, boolean wasCreated) {}

    /**
     * Creates an empty group with the given name.
     * If the name already exists (including concurrent create races), returns the existing group
     * with {@code wasCreated = false}.
     *
     * @param name the group name (must not be null or blank)
     * @param auditInfo audit context
     * @return result containing the group and whether it was newly created
     * @throws IllegalArgumentException if name is null or blank
     */
    CreateResult createEmpty(String name, AuditInfo auditInfo);
}
