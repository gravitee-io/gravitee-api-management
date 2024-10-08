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
import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@DomainService
@RequiredArgsConstructor
public class ValidateGroupsDomainService implements Validator<ValidateGroupsDomainService.Input> {

    public record Input(String environmentId, Set<String> groups, String definitionVersion) implements Validator.Input {
        Input sanitized(Set<String> sanitizedGroups) {
            return new Input(environmentId, sanitizedGroups, definitionVersion);
        }
    }

    private final GroupQueryService groupQueryService;

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        if (isEmpty(input.groups())) {
            log.debug("no group to resolve");
            return Result.ofValue(input);
        }
        log.debug("resolving groups");

        var givenGroups = new HashSet<>(input.groups());
        var sanitizedGroups = new HashSet<String>();

        var groupsFromIds = groupQueryService.findByIds(givenGroups).stream().toList();
        var groupsFromNames = groupQueryService.findByNames(input.environmentId(), givenGroups).stream().toList();

        var groupIds = groupsFromIds.stream().map(Group::getId).collect(toSet());
        var groupNames = groupsFromNames.stream().map(Group::getName).collect(toSet());

        var errors = new ArrayList<Error>();

        if (DefinitionVersion.V2.getLabel().equals(input.definitionVersion)) {
            sanitizedGroups.addAll(groupsFromIds.stream().map(Group::getName).toList());
            sanitizedGroups.addAll(groupsFromNames.stream().map(Group::getName).toList());
        } else {
            sanitizedGroups.addAll(groupsFromIds.stream().map(Group::getId).toList());
            sanitizedGroups.addAll(groupsFromNames.stream().map(Group::getId).toList());
        }

        givenGroups.removeAll(groupIds);
        givenGroups.removeAll(groupNames);

        for (var unknownGroup : givenGroups) {
            errors.add(Error.warning("Group [%s] could not be found in environment [%s]", unknownGroup, input.environmentId()));
        }

        return Result.ofBoth(input.sanitized(sanitizedGroups), errors);
    }
}
