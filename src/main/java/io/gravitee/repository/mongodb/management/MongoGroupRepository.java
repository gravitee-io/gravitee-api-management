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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.mongodb.management.internal.group.GroupMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.GroupMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
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
public class MongoGroupRepository implements GroupRepository {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private GroupMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Group> findById(String s) throws TechnicalException {
        logger.debug("Find group by id [{}]", s);
        Group group = map(internalRepository.findById(s).orElse(null));
        logger.debug("Find group by id [{}] - DONE", s);
        return Optional.ofNullable(group);
    }

    @Override
    public Set<Group> findByIds(Set<String> ids) throws TechnicalException {
        logger.debug("Find groups by ids");
        Set<Group> groups = internalRepository.findByIds(ids)
                .stream()
                .map(this::map)
                .collect(Collectors.toSet());
        logger.debug("Find groups by ids - Found {}", groups);
        return groups;
    }

    @Override
    public Group create(Group group) throws TechnicalException {
        logger.debug("Create group [{}]", group.getName());
        Group createdGroup = map(internalRepository.insert(map(group)));
        logger.debug("Create group [{}] - Done", createdGroup.getName());
        return createdGroup;
    }

    @Override
    public Group update(Group group) throws TechnicalException {
        if (group == null) {
            throw new IllegalStateException("Group must not be null");
        }

        final GroupMongo groupMongo = internalRepository.findById(group.getId()).orElse(null);
        if (groupMongo == null) {
            throw new IllegalStateException(String.format("No group found with id [%s]", group.getId()));
        }
        
        logger.debug("Update group [{}]", group.getName());
        Group updatedGroup = map(internalRepository.save(map(group)));
        logger.debug("Update group [{}] - Done", updatedGroup.getName());
        return updatedGroup;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("Delete group [{}]", id);
        internalRepository.deleteById(id);
        logger.debug("Delete group [{}] - Done", id);
    }

    @Override
    public Set<Group> findAll() throws TechnicalException {
        logger.debug("Find all groups");
        Set<Group> all = internalRepository.findAll().
                stream().
                map(this::map).
                collect(Collectors.toSet());
        logger.debug("Find all groups - Found {}", all);
        return all;
    }

    private GroupMongo map(Group group) {
        GroupMongo mongoGroup =  mapper.map(group, GroupMongo.class);

        if (group != null && group.getRoles() != null) {
            List<String> roles = new ArrayList<>(group.getRoles().size());
            for (Map.Entry<Integer, String> roleEntry : group.getRoles().entrySet()) {
                roles.add(convertRoleToType(roleEntry.getKey(), roleEntry.getValue()));
            }

            mongoGroup.setRoles(roles);
        }

        return mongoGroup;
    }

    private Group map(GroupMongo groupMongo) {
        Group group = mapper.map(groupMongo, Group.class);

        if (groupMongo != null && groupMongo.getRoles() != null) {
            Map<Integer, String> roles = new HashMap<>(groupMongo.getRoles().size());
            for (String roleAsString : groupMongo.getRoles()) {
                String[] role = convertTypeToRole(roleAsString);
                roles.put(Integer.valueOf(role[0]), role[1]);
            }
            group.setRoles(roles);
        }

        return group;
    }

    private String convertRoleToType(RoleScope roleScope, String roleName) {
        if (roleName == null) {
            return null;
        }
        return convertRoleToType(roleScope.getId(), roleName);
    }

    private String convertRoleToType(int roleScope, String roleName) {
        return roleScope + ":" + roleName;
    }

    private String[] convertTypeToRole(String type) {
        if (type == null) {
            return null;
        }
        String[] role = type.split(":");
        if (role.length != 2) {
            return null;
        }
        return role;
    }

    private Set<Group> collection2set(Collection<GroupMongo> groups) {
        return mapper.collection2set(groups, GroupMongo.class, Group.class);
    }

    @Override
    public Set<Group> findAllByEnvironment(String environment) throws TechnicalException {
        logger.debug("Find all groups by environment");
        Set<Group> all = internalRepository.findByEnvironment(environment).
                stream().
                map(this::map).
                collect(Collectors.toSet());
        logger.debug("Find all groups by environment - Found {}", all);
        return all;
    }
}
