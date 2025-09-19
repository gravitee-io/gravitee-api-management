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
package io.gravitee.apim.infra.query_service.group;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.infra.adapter.GroupAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.search.GroupCriteria;
import io.gravitee.repository.management.model.GroupEventRule;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GroupQueryServiceImpl extends AbstractService implements GroupQueryService {

    private final GroupRepository groupRepository;

    public GroupQueryServiceImpl(@Lazy GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public Optional<Group> findById(String id) {
        try {
            return groupRepository.findById(id).map(GroupAdapter.INSTANCE::toModel);
        } catch (TechnicalException ex) {
            throw new TechnicalDomainException("An error occurs while trying to find group by id: " + id, ex);
        }
    }

    @Override
    public Set<Group> findByIds(Set<String> ids) {
        try {
            log.debug("findByIds {}", ids);
            Set<io.gravitee.repository.management.model.Group> groups = groupRepository.findByIds(ids);
            return groups
                .stream()
                .map(GroupAdapter.INSTANCE::toModel)
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (TechnicalException ex) {
            throw new TechnicalDomainException("An error occurs while trying to find groups", ex);
        }
    }

    @Override
    public Set<Group> findByEvent(String environmentId, Group.GroupEvent event) {
        try {
            log.debug("findByEvent {}", event);
            Set<io.gravitee.repository.management.model.Group> groups = groupRepository.findAllByEnvironment(environmentId);
            return groups
                .stream()
                .filter(
                    g ->
                        g.getEventRules() != null &&
                        g
                            .getEventRules()
                            .stream()
                            .map(GroupEventRule::getEvent)
                            .anyMatch(repoEvent -> repoEvent == GroupAdapter.INSTANCE.mapEvent(event))
                )
                .map(GroupAdapter.INSTANCE::toModel)
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (TechnicalException ex) {
            throw new TechnicalDomainException("An error occurs while trying to find groups by event", ex);
        }
    }

    @Override
    public List<Group> findByNames(String environmentId, Set<String> names) {
        try {
            log.debug("findByNames {}", names);
            if (CollectionUtils.isEmpty(names)) {
                return List.of();
            }
            return groupRepository
                .findAllByEnvironment(environmentId)
                .stream()
                .filter(group -> names.contains(group.getName()))
                .map(GroupAdapter.INSTANCE::toModel)
                .sorted(Comparator.comparing(Group::getName))
                .toList();
        } catch (TechnicalException ex) {
            throw new TechnicalDomainException("An error occurs while trying to find groups by names", ex);
        }
    }

    @Override
    public Page<Group> searchGroups(ExecutionContext executionContext, Set<String> groupIds, Pageable pageable) {
        return groupRepository
            .search(GroupCriteria.builder().idIn(groupIds).build(), convert(pageable))
            .map(GroupAdapter.INSTANCE::toModel);
    }
}
