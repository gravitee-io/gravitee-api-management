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
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
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

    public record Input(
        String environmentId,
        Set<String> groups,
        String definitionVersion,
        Group.GroupEvent groupEvent,
        boolean addDefaultGroups
    ) implements Validator.Input {
        Input sanitized(Set<String> sanitizedGroups) {
            return new Input(environmentId, sanitizedGroups, definitionVersion, groupEvent, addDefaultGroups);
        }
    }

    private final GroupQueryService groupQueryService;

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var sanitizedGroups = new HashSet<String>();

        if (input.addDefaultGroups) {
            log.debug("find default groups");
            Set<String> defaultGroups = groupQueryService
                .findByEvent(input.environmentId, input.groupEvent)
                .stream()
                .map(Group::getId)
                .collect(toSet());
            sanitizedGroups.addAll(defaultGroups);
        }

        if (isEmpty(input.groups())) {
            log.debug("no group to resolve");

            // just to be backward compatible with Integration team
            var value = isEmpty(sanitizedGroups) ? input : input.sanitized(sanitizedGroups);
            return Result.ofBoth(value, null);
        }
        log.debug("resolving groups");

        var givenGroups = new HashSet<>(input.groups());

        var groupsFromIds = groupQueryService.findByIds(givenGroups).stream().toList();
        var groupsFromNames = groupQueryService.findByNames(input.environmentId(), givenGroups).stream().toList();

        var groupIds = groupsFromIds.stream().map(Group::getId).collect(toSet());
        var groupNames = groupsFromNames.stream().map(Group::getName).collect(toSet());

        var errors = new ArrayList<Error>();

        var noPrimaryOwnerResultFromIds = validateAndSanitizeNoPrimaryOwners(groupsFromIds, Group::getId);
        var noPrimaryOwnerResultFromNames = validateAndSanitizeNoPrimaryOwners(groupsFromNames, Group::getName);

        noPrimaryOwnerResultFromIds.errors().ifPresent(errors::addAll);
        noPrimaryOwnerResultFromNames.errors().ifPresent(errors::addAll);

        var sanitizedFromIds = noPrimaryOwnerResultFromIds.value().orElse(List.of());
        var sanitizedFromNames = noPrimaryOwnerResultFromNames.value().orElse(List.of());

        if (DefinitionVersion.V2.getLabel().equals(input.definitionVersion)) {
            sanitizedGroups.addAll(sanitizedFromIds.stream().map(Group::getName).toList());
            sanitizedGroups.addAll(sanitizedFromNames.stream().map(Group::getName).toList());
        } else {
            sanitizedGroups.addAll(sanitizedFromIds.stream().map(Group::getId).toList());
            sanitizedGroups.addAll(sanitizedFromNames.stream().map(Group::getId).toList());
        }

        givenGroups.removeAll(groupIds);
        givenGroups.removeAll(groupNames);

        for (var unknownGroup : givenGroups) {
            errors.add(Error.warning("Group [%s] could not be found in environment [%s]", unknownGroup, input.environmentId()));
        }

        return Result.ofBoth(input.sanitized(sanitizedGroups), errors);
    }

    private Result<List<Group>> validateAndSanitizeNoPrimaryOwners(List<Group> groups, Function<Group, String> idMapper) {
        var sanitized = new ArrayList<>(groups);
        var groupsWithPrimaryOwner = sanitized
            .stream()
            .filter(group -> StringUtils.isNotEmpty(group.getApiPrimaryOwner()))
            .toList();
        sanitized.removeAll(groupsWithPrimaryOwner);
        var errors = buildPrimaryOwnerErrors(groupsWithPrimaryOwner, idMapper);
        return Result.ofBoth(sanitized, errors);
    }

    private List<Error> buildPrimaryOwnerErrors(List<Group> groupsWithPrimaryOwner, Function<Group, String> idMapper) {
        return groupsWithPrimaryOwner
            .stream()
            .map(idMapper)
            .map(id ->
                Error.warning(
                    "Group [%s] will be discarded because it contains an API Primary Owner member, which is not supported with by the operator.",
                    id
                )
            )
            .toList();
    }
}
