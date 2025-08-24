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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.group.model.Group.GroupEventRule;
import io.gravitee.rest.api.management.v2.rest.model.Group;
import io.gravitee.rest.api.management.v2.rest.model.GroupEvent;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.GroupEventRuleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { DateMapper.class })
public interface GroupMapper {
    Logger logger = LoggerFactory.getLogger(GroupMapper.class);
    GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);

    @Mapping(source = "roles", target = "apiRole", qualifiedByName = "mapApiRole")
    @Mapping(source = "roles", target = "applicationRole", qualifiedByName = "mapApplicationRole")
    Group map(GroupEntity group);

    List<Group> map(List<GroupEntity> groups);

    default List<GroupEvent> mapGroupEventRuleEntities(List<GroupEventRuleEntity> events) {
        if (Objects.isNull(events)) {
            return null;
        }
        return events
            .stream()
            .map(event -> {
                if (Objects.nonNull(event)) {
                    try {
                        return GroupEvent.fromValue(event.getEvent());
                    } catch (IllegalArgumentException e) {
                        logger.error("Unable to parse GroupEventRuleEntity: " + event.getEvent());
                    }
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    @Named("mapApiRole")
    default String mapApiRole(Map<RoleScope, String> roles) {
        if (Objects.isNull(roles)) {
            return null;
        }
        return roles.get(RoleScope.API);
    }

    @Named("mapApplicationRole")
    default String mapApplicationRole(Map<RoleScope, String> roles) {
        if (Objects.isNull(roles)) {
            return null;
        }
        return roles.get(RoleScope.APPLICATION);
    }

    List<Group> mapFromCoreList(List<io.gravitee.apim.core.group.model.Group> coreGroups);

    Group mapFromCore(io.gravitee.apim.core.group.model.Group coreGroup);

    default List<GroupEvent> mapCoreGroupEventRules(List<GroupEventRule> eventRules) {
        if (Objects.isNull(eventRules)) {
            return null;
        }

        return eventRules
            .stream()
            .filter(Objects::nonNull)
            .map(rule -> {
                try {
                    return GroupEvent.fromValue(rule.event().name());
                } catch (IllegalArgumentException e) {
                    logger.error("Unable to parse GroupEventRule: " + rule.event().name());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
