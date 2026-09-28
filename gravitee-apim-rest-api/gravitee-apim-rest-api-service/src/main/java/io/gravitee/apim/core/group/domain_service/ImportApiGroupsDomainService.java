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

import static io.gravitee.apim.core.utils.CollectionUtils.isEmpty;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Resolves API membership groups for import/promotion:
 * match groups in the target environment by name; auto-create an empty group when missing.
 * <p>
 * Group refs in the definition are treated as <strong>names</strong>.
 * Creation uses {@link CreateGroupDomainService} for unique-name enforcement and {@code GROUP_CREATED} audit.
 * <p>
 * <strong>Migration:</strong> V4 exports produced before this change stored group IDs. Re-importing such a file
 * treats each ID string as a name—if no group with that name exists, an empty group named like the UUID is
 * created. Prefer re-exporting after upgrade, or rename/replace those entries with real group names before import.
 */
@RequiredArgsConstructor
@CustomLog
@DomainService
public class ImportApiGroupsDomainService {

    private final GroupQueryService groupQueryService;
    private final CreateGroupDomainService createGroupDomainService;

    public Set<String> resolveOrCreateGroupIds(Set<String> groupRefs, AuditInfo auditInfo) {
        if (isEmpty(groupRefs)) {
            return groupRefs;
        }

        var environmentId = auditInfo.environmentId();
        var validGroupNames = groupRefs
            .stream()
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.toSet());

        if (validGroupNames.isEmpty()) {
            return Set.of();
        }

        var resolvedByName = new HashMap<String, Group>();
        var missingNames = new HashSet<>(validGroupNames);

        resolveExistingByName(resolvedByName, missingNames, environmentId);

        for (String missingGroupName : missingNames) {
            var result = createGroupDomainService.createEmpty(missingGroupName, auditInfo);
            resolvedByName.put(missingGroupName, result.group());
            if (result.wasCreated()) {
                log.warn(
                    "Group [{}] did not exist in environment [{}] and was created empty during API import/promotion. " +
                        "Members and group configuration were not copied.",
                    missingGroupName,
                    environmentId
                );
            }
        }

        return resolvedByName.values().stream().map(Group::getId).collect(Collectors.toSet());
    }

    private void resolveExistingByName(Map<String, Group> resolvedByName, Set<String> missingNames, String environmentId) {
        if (missingNames.isEmpty()) {
            return;
        }
        var foundByNames = groupQueryService.findByNames(environmentId, missingNames);
        // Use first group found for each name (handles potential duplicates)
        for (Group group : foundByNames) {
            if (!resolvedByName.containsKey(group.getName())) {
                resolvedByName.put(group.getName(), group);
            }
        }
        missingNames.removeAll(resolvedByName.keySet());
    }
}
