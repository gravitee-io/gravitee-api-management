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
package inmemory;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.CreateGroupDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * In-memory create that mirrors V2 unique-name reuse: if the name already exists in the
 * environment, return it instead of creating a duplicate.
 */
public class CreateGroupDomainServiceInMemory implements CreateGroupDomainService {

    private final GroupQueryServiceInMemory groupQueryService;
    private final HashMap<String, Group> created = new HashMap<>();

    public CreateGroupDomainServiceInMemory(GroupQueryServiceInMemory groupQueryService) {
        this.groupQueryService = groupQueryService;
    }

    @Override
    public CreateResult createEmpty(String name, AuditInfo auditInfo) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name must not be null or blank");
        }
        var existing = groupQueryService.findByNames(auditInfo.environmentId(), Set.of(name));
        if (!existing.isEmpty()) {
            return new CreateResult(existing.getFirst(), false);
        }
        var now = TimeProvider.now();
        var group = Group.builder()
            .id(UuidString.generateRandom())
            .environmentId(auditInfo.environmentId())
            .name(name)
            .createdAt(now)
            .updatedAt(now)
            .build();
        created.put(group.getId(), group);
        groupQueryService.initWith(List.of(group));
        return new CreateResult(group, true);
    }

    public HashMap<String, Group> storage() {
        return created;
    }

    public void reset() {
        created.clear();
    }
}
