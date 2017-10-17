/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.exceptions.GroupNameAlreadyExistsException;
import io.gravitee.management.service.exceptions.GroupNotFoundException;
import io.gravitee.management.service.exceptions.GroupsNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class GroupServiceImpl extends TransactionalService implements GroupService {
    private final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private MembershipService membershipService;

    @Override
    public List<GroupEntity> findAll() {
        try {
            logger.debug("Find all groups");
            Set<Group> all = groupRepository.findAll();
            logger.debug("Find all groups - DONE");
            return all.stream()
                    .map(this::map)
                    .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find all groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all groups", ex);
        }
    }

    @Override
    public List<GroupEntity> findByName(String name) {
        try {
            logger.debug("findByName : {}", name);
            if (name == null) {
                return Collections.emptyList();
            }
            List<GroupEntity> groupEntities = groupRepository.findAll().stream()
                    .filter(group -> group.getName().equals(name))
                    .map(this::map)
                    .collect(Collectors.toList());
            logger.debug("findByName : {} - DONE", name);
            return groupEntities;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups by name", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups by name", ex);
        }
    }

    @Override
    public GroupEntity create(NewGroupEntity group) {
        try {
            logger.debug("create {}", group);
            if( !this.findByName(group.getName()).isEmpty()) {
                throw new GroupNameAlreadyExistsException(group.getName());
            }
            Group newGroup = this.map(group);
            newGroup.setId(UUID.toString(UUID.random()));
            newGroup.setCreatedAt(new Date());
            newGroup.setUpdatedAt(newGroup.getCreatedAt());
            GroupEntity grp = this.map(groupRepository.create(newGroup));
            logger.debug("create {} - DONE", grp);
            return grp;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to create a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to create a group", ex);
        }
    }

    @Override
    public GroupEntity update(String groupId, UpdateGroupEntity group) {
        try {
            logger.debug("update {}", group);
            GroupEntity updatedGroup = this.findById(groupId);
            updatedGroup.setName(group.getName());
            updatedGroup.setUpdatedAt(new Date());
            updatedGroup.setEventRules(group.getEventRules());
            GroupEntity grp = this.map(groupRepository.update(this.map(updatedGroup)));
            logger.debug("update {} - DONE", grp);
            return grp;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to update a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to update a group", ex);
        }
    }

    @Override
    public GroupEntity findById(String groupId) {
        try {
            logger.debug("findById {}", groupId);
            Optional<Group> group = groupRepository.findById(groupId);
            if (!group.isPresent()) {
                throw new GroupNotFoundException(groupId);
            }
            logger.debug("findById {} - DONE", group.get());
            return this.map(group.get());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to find a group", ex);
        }
    }

    @Override
    public Set<GroupEntity> findByIds(Set<String> groupIds) {
        try {
            logger.debug("findByIds {}", groupIds);
            Set<Group> groups = groupRepository.findByIds(groupIds);
            if (groups == null || groups.size() != groupIds.size()) {
                List<String> groupsFound = groups == null ? Collections.emptyList()
                        : groups.stream().map(Group::getId).collect(Collectors.toList());
                Set<String> groupIdsNotFound = new HashSet<>(groupIds);
                groupIdsNotFound.removeAll(groupsFound);
                throw new GroupsNotFoundException(groupIdsNotFound);
            }
            logger.debug("findByIds {} - DONE", groups);
            return groups.
                    stream().
                    map(this::map).
                    collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups", ex);
        }
    }

    @Override
    public Set<GroupEntity> findByEvent(GroupEvent event) {
        try {
            logger.debug("findByEvent : {}", event);
            Set<GroupEntity> set = groupRepository.findAll().
                    stream().
                    filter(g -> g.getEventRules() != null && g.getEventRules().
                            stream().
                            map(GroupEventRule::getEvent).
                            collect(Collectors.toList()).
                            contains(event)).
                    map(this::map).
                    collect(Collectors.toSet());
            logger.debug("findByEvent : {} - DONE", set);
            return set;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups by event", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups by event", ex);
        }
    }

    @Override
    public void delete(String groupId) {
        try {
            logger.debug("delete {}", groupId);
            GroupEntity groupEntity = this.findById(groupId);
            //remove all members
            membershipRepository.findByReferenceAndRole(
                    MembershipReferenceType.GROUP,
                    groupId,
                    null,
                    null)
                    .forEach(member -> {
                        try {
                            membershipRepository.delete(member);
                        } catch (TechnicalException ex) {
                            logger.error("An error occurs while trying to delete a group", ex);
                            throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                        }
                    });

            //remove all applications or apis
            Date updatedDate = new Date();
            apiRepository.findByGroups(Collections.singletonList(groupId)).forEach( api -> {
                api.getGroups().remove(groupId);
                api.setUpdatedAt(updatedDate);
                try {
                    apiRepository.update(api);
                } catch (TechnicalException ex) {
                    logger.error("An error occurs while trying to delete a group", ex);
                    throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                }
            });
            applicationRepository.findByGroups(Collections.singletonList(groupId)).forEach( application -> {
                application.getGroups().remove(groupId);
                application.setUpdatedAt(updatedDate);
                try {
                    applicationRepository.update(application);
                } catch (TechnicalException ex) {
                    logger.error("An error occurs while trying to delete a group", ex);
                    throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                }
            });
            //remove group
            groupRepository.delete(groupId);
            logger.debug("delete {} - DONE", groupId);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
        }

    }

    @Override
    public boolean isUserAuthorizedToAccessApiData(ApiEntity api, List<String> excludedGroups, String username) {
        // in anonymous mode, only public API without restrictions are authorized
        if (username == null) {
            return (excludedGroups == null || excludedGroups.isEmpty())
                    && (Visibility.PUBLIC.equals(api.getVisibility()));
        }

        if ( // plan contains excluded groups
             excludedGroups != null && !excludedGroups.isEmpty()
             // user is not directly member of the API
             && membershipService.getMember(MembershipReferenceType.API, api.getId(), username, RoleScope.API) == null
           ) {

            // for public apis, default authorized groups are all groups,
            // for private apis, default authorized groups are all apis groups
            Set<String> authorizedGroups = Collections.emptySet();
            if (Visibility.PRIVATE.equals(api.getVisibility()) && api.getGroups() != null && !api.getGroups().isEmpty()) {
                authorizedGroups = new HashSet<>(api.getGroups());
            }
            if (Visibility.PUBLIC.equals(api.getVisibility())) {
                try {
                    authorizedGroups = groupRepository.findAll().stream().map(Group::getId).collect(Collectors.toSet());
                } catch (TechnicalException ex) {
                    logger.error("An error occurs while trying to find all groups", ex);
                    throw new TechnicalManagementException("An error occurs while trying to find all groups", ex);
                }
            }

            authorizedGroups.removeAll(excludedGroups);
            for (String authorizedGroupId : authorizedGroups) {
                if (membershipService.getMember(MembershipReferenceType.GROUP, authorizedGroupId, username, RoleScope.API) != null) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean isUserAuthorizedToAccessPortalData(List<String> excludedGroups, String username) {
        // in anonymous mode, only pages without restrictions are authorized
        if (username == null) {
            return (excludedGroups == null || excludedGroups.isEmpty());
        }

        if (excludedGroups != null && !excludedGroups.isEmpty()) {
            // for public apis, default authorized groups are all groups,
            Set<String> authorizedGroups;
            try {
                authorizedGroups = groupRepository.findAll().stream().map(Group::getId).collect(Collectors.toSet());
            } catch (TechnicalException ex) {
                logger.error("An error occurs while trying to find all groups", ex);
                throw new TechnicalManagementException("An error occurs while trying to find all groups", ex);
            }

            authorizedGroups.removeAll(excludedGroups);
            for (String authorizedGroupId : authorizedGroups) {
                if (membershipService.getMember(MembershipReferenceType.GROUP, authorizedGroupId, username, RoleScope.API) != null) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private Group map(GroupEntity entity) {
        if (entity == null) {
            return null;
        }

        Group group = new Group();
        group.setId(entity.getId());
        group.setName(entity.getName());
        if(entity.getEventRules() != null && !entity.getEventRules().isEmpty()) {
            List<GroupEventRule> groupEventRules = new ArrayList<>();
            for (GroupEventRuleEntity groupEventRuleEntity : entity.getEventRules()) {
                GroupEventRule eventRule = new GroupEventRule();
                eventRule.setEvent(GroupEvent.valueOf(groupEventRuleEntity.getEvent()));
                groupEventRules.add(eventRule);
            }
            group.setEventRules(groupEventRules);
        }
        group.setCreatedAt(entity.getCreatedAt());
        group.setUpdatedAt(entity.getUpdatedAt());

        return group;
    }

    private Group map(NewGroupEntity entity) {
        if (entity == null) {
            return null;
        }

        Group group = new Group();
        group.setName(entity.getName());
        if(entity.getEventRules() != null && !entity.getEventRules().isEmpty()) {
            List<GroupEventRule> groupEventRules = new ArrayList<>();
            for (GroupEventRuleEntity groupEventRuleEntity : entity.getEventRules()) {
                GroupEventRule eventRule = new GroupEventRule();
                eventRule.setEvent(GroupEvent.valueOf(groupEventRuleEntity.getEvent()));
                groupEventRules.add(eventRule);
            }
            group.setEventRules(groupEventRules);
        }
        return group;
    }

    private GroupEntity map(Group group) {
        if (group == null) {
            return null;
        }

        GroupEntity entity = new GroupEntity();
        entity.setId(group.getId());
        entity.setName(group.getName());
        if(group.getEventRules() != null && !group.getEventRules().isEmpty()) {
            List<GroupEventRuleEntity> groupEventRules = new ArrayList<>();
            for (GroupEventRule groupEventRule : group.getEventRules()) {
                GroupEventRuleEntity eventRuleEntity = new GroupEventRuleEntity();
                eventRuleEntity.setEvent(groupEventRule.getEvent().name());
                groupEventRules.add(eventRuleEntity);
            }
            entity.setEventRules(groupEventRules);
        }
        entity.setCreatedAt(group.getCreatedAt());
        entity.setUpdatedAt(group.getUpdatedAt());

        return entity;
    }
}
