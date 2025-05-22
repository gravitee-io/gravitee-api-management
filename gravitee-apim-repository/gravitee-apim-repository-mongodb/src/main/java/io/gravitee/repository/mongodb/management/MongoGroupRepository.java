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
package io.gravitee.repository.mongodb.management;

import static io.gravitee.repository.mongodb.utils.CollectionUtils.stream;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.search.GroupCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.mongodb.management.internal.group.GroupMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.GroupMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoGroupRepository implements GroupRepository {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Autowired
    private GroupMongoRepository internalRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

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
        Set<Group> groups = stream(internalRepository.findByIds(ids)).map(this::map).collect(Collectors.toSet());
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
        Set<Group> all = internalRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
        logger.debug("Find all groups - Found {}", all);
        return all;
    }

    private GroupMongo map(Group group) {
        return mapper.map(group);
    }

    private Group map(GroupMongo groupMongo) {
        return mapper.map(groupMongo);
    }

    @Override
    public Page<Group> search(GroupCriteria groupCriteria, Pageable pageable) {
        Query query = new Query();
        if (StringUtils.hasText(groupCriteria.getEnvironmentId())) {
            query.addCriteria(where("environmentId").is(groupCriteria.getEnvironmentId()));
        }
        if (!CollectionUtils.isEmpty(groupCriteria.getIdIn())) {
            query.addCriteria(where("id").in(groupCriteria.getIdIn()));
        }
        if (StringUtils.hasText(groupCriteria.getQuery())) {
            query.addCriteria(where("name").regex(groupCriteria.getQuery(), "i"));
        }
        org.springframework.data.domain.Pageable dbPageable = PageRequest.of(
            pageable.pageNumber(),
            pageable.pageSize(),
            Sort.by(Sort.Order.asc("name"))
        );
        long total = mongoTemplate.count(Query.of(query), GroupMongo.class);
        List<Group> groups = mongoTemplate.find(query.with(dbPageable), GroupMongo.class).stream().map(mapper::map).toList();
        return new Page<>(groups, pageable.pageNumber(), pageable.pageSize(), total);
    }

    @Override
    public Set<Group> findAllByEnvironment(String environmentId) throws TechnicalException {
        logger.debug("Find all groups by environment");
        Set<Group> all = internalRepository.findByEnvironmentId(environmentId).stream().map(this::map).collect(Collectors.toSet());
        logger.debug("Find all groups by environment - Found {}", all);
        return all;
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        logger.debug("Delete groups by environmentId: {}", environmentId);
        try {
            List<String> all = internalRepository.deleteByEnvironmentId(environmentId).stream().map(GroupMongo::getId).toList();
            logger.debug("Delete groups by environment - Done {}", all);
            return all;
        } catch (Exception ex) {
            logger.error("Failed to delete groups by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete groups by environmentId");
        }
    }

    @Override
    public Set<Group> findAllByOrganization(String organizationId) throws TechnicalException {
        logger.debug("Find all groups by organization");

        LookupOperation lookupOperation = LookupOperation
            .newLookup()
            .from(getTableNameFor("environments"))
            .localField("environmentId")
            .foreignField("_id")
            .as("environment");

        Aggregation aggregation = Aggregation.newAggregation(
            lookupOperation,
            Aggregation.match(where("environment.organizationId").is(organizationId))
        );

        Set<Group> groups = mongoTemplate
            .aggregate(aggregation, getTableNameFor("groups"), GroupMongo.class)
            .getMappedResults()
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());

        logger.debug("Find all groups by organization - Found {}", groups);
        return groups;
    }

    private String getTableNameFor(String table) {
        return tablePrefix + table;
    }
}
