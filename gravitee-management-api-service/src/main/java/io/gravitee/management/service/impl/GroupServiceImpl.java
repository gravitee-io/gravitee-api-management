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
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.exceptions.GroupNameAlreadyExistsException;
import io.gravitee.management.service.exceptions.GroupNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.MembershipReferenceType;
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
    public List<GroupEntity> findByType(GroupEntityType type) {
        try {
            logger.debug("findByType : {}", type);
            if (type == null) {
                return Collections.emptyList();
            }
            List<GroupEntity> groupEntities = groupRepository.findByType(Group.Type.valueOf(type.name())).stream()
                    .map(this::map)
                    .collect(Collectors.toList());
            logger.debug("findByType : {} - DONE", type);
            return groupEntities;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups by Type", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups by Type", ex);
        }
    }

    @Override
    public List<GroupEntity> findByTypeAndName(GroupEntityType type, String name) {
        try {
            logger.debug("findByTypeAndName : {}, {}", type, name);
            if (type == null || name == null) {
                return Collections.emptyList();
            }
            List<GroupEntity> groupEntities = groupRepository.findByType(Group.Type.valueOf(type.name())).stream()
                    .filter(group -> group.getName().equals(name))
                    .map(this::map)
                    .collect(Collectors.toList());
            logger.debug("findByType : {} - DONE", type);
            return groupEntities;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups by Type", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups by Type", ex);
        }
    }

    @Override
    public GroupEntity create(NewGroupEntity group) {
        try {
            logger.debug("create {}", group);
            if( !this.findByTypeAndName(group.getType(), group.getName()).isEmpty()) {
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
    public void delete(String groupId) {
        try {
            logger.debug("delete {}", groupId);
            GroupEntity groupEntity = this.findById(groupId);
            //remove all members
            membershipRepository.findByReferenceAndMembershipType(
                    groupEntity.getType().equals(GroupEntityType.API)
                            ? MembershipReferenceType.API_GROUP
                            : MembershipReferenceType.APPLICATION_GROUP,
                    groupId,
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
            if (groupEntity.getType().equals(GroupEntityType.API)) {
                apiRepository.findByGroups(Collections.singletonList(groupId)).forEach( api -> {
                    api.setGroup(null);
                    api.setUpdatedAt(updatedDate);
                    try {
                        apiRepository.update(api);
                    } catch (TechnicalException ex) {
                        logger.error("An error occurs while trying to delete a group", ex);
                        throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                    }
                });
            } else {
                applicationRepository.findByGroups(Collections.singletonList(groupId)).forEach( application -> {
                    application.setGroup(null);
                    application.setUpdatedAt(updatedDate);
                    try {
                        applicationRepository.update(application);
                    } catch (TechnicalException ex) {
                        logger.error("An error occurs while trying to delete a group", ex);
                        throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                    }
                });
            }
            //remove group
            groupRepository.delete(groupId);
            logger.debug("delete {} - DONE", groupId);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
        }

    }

    private Group map(GroupEntity entity) {
        if (entity == null) {
            return null;
        }

        Group group = new Group();
        group.setId(entity.getId());
        group.setType(Group.Type.valueOf(entity.getType().name()));
        group.setName(entity.getName());
        group.setCreatedAt(entity.getCreatedAt());
        group.setUpdatedAt(entity.getUpdatedAt());

        return group;
    }

    private Group map(NewGroupEntity entity) {
        if (entity == null) {
            return null;
        }

        Group group = new Group();
        group.setType(Group.Type.valueOf(entity.getType().name()));
        group.setName(entity.getName());

        return group;
    }

    private GroupEntity map(Group group) {
        if (group == null) {
            return null;
        }

        GroupEntity entity = new GroupEntity();
        entity.setId(group.getId());
        entity.setType(GroupEntityType.valueOf(group.getType().name()));
        entity.setName(group.getName());
        entity.setCreatedAt(group.getCreatedAt());
        entity.setUpdatedAt(group.getUpdatedAt());

        return entity;
    }
}
