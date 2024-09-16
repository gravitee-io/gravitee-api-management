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
package io.gravitee.apim.core.documentation.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@DomainService
@AllArgsConstructor
public class ValidatePageAccessControlsDomainService implements Validator<ValidatePageAccessControlsDomainService.Input> {

    public record Input(AuditInfo auditInfo, Set<AccessControl> accessControls) implements Validator.Input {
        Input sanitized(Set<AccessControl> accessControls) {
            return new Input(auditInfo, accessControls);
        }
    }

    private final GroupQueryService groupQueryService;

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        if (input.accessControls == null) {
            log.debug("returning an empty validation result as there are no access controls to sanitize");
            return Result.empty();
        }
        var sanitized = new HashSet<>(sanitizeGroupIds(input.auditInfo.environmentId(), input.accessControls));
        return Result.ofValue(input.sanitized(sanitized));
    }

    private Set<AccessControl> sanitizeGroupIds(String environmentId, Set<AccessControl> accessControls) {
        var acl = new HashSet<>(accessControls.stream().filter(AccessControl::isGroup).toList());
        var groupIdToReference = mapGroupIdToReference(environmentId, acl);

        var it = acl.iterator();
        while (it.hasNext()) {
            var ac = it.next();
            var id = groupIdToReference.get(ac.getReferenceId());
            if (id != null) {
                ac.setReferenceId(id);
            } else {
                log.debug("removing access control {} as it does not match a valid reference", ac);
                it.remove();
            }
        }
        return acl;
    }

    private Map<String, String> mapGroupIdToReference(String environmentId, Set<AccessControl> accessControls) {
        var references = accessControls.stream().map(AccessControl::getReferenceId).collect(Collectors.toSet());

        var byIds = this.groupQueryService.findByIds(references).stream().collect(Collectors.toMap(Group::getId, Group::getId));

        var byNames =
            this.groupQueryService.findByNames(environmentId, references).stream().collect(Collectors.toMap(Group::getName, Group::getId));

        var mapping = new HashMap<>(byIds);
        mapping.putAll(byNames);
        return mapping;
    }
}
