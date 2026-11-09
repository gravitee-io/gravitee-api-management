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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GuardianRepository;
import io.gravitee.repository.management.api.search.GuardianCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Guardian;
import io.gravitee.repository.mongodb.management.internal.guardians.GuardianMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.GuardianMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
@RequiredArgsConstructor
public class MongoGuardianRepository implements GuardianRepository {

    private final GraviteeMapper mapper;
    private final GuardianMongoRepository internalGuardianMongoRepo;

    @Override
    public Optional<Guardian> findById(String id) throws TechnicalException {
        log.debug("Find guardian by ID [{}]", id);
        final GuardianMongo guardianMongo = internalGuardianMongoRepo.findById(id).orElse(null);
        log.debug("Find guardian by ID [{}] - Done", id);
        return Optional.ofNullable(mapGuardian(guardianMongo));
    }

    @Override
    public Optional<Guardian> findByEnvironmentIdAndNameIgnoreCase(String environmentId, String name) throws TechnicalException {
        Objects.requireNonNull(environmentId, "EnvironmentId must not be null");
        Objects.requireNonNull(name, "Name must not be null");
        log.debug("Find guardian by environment [{}] and name", environmentId);
        return internalGuardianMongoRepo
            .findByEnvironmentIdAndNameIgnoreCase(environmentId, name)
            .stream()
            .findFirst()
            .map(this::mapGuardian);
    }

    @Override
    public Guardian create(Guardian item) throws TechnicalException {
        log.debug("Create guardian with id [{}]", item.getId());
        final GuardianMongo created = internalGuardianMongoRepo.insert(mapGuardian(item));
        final Guardian res = mapGuardian(created);
        log.debug("Create guardian with id [{}] - Done", res.getId());
        return res;
    }

    @Override
    public Guardian update(Guardian item) throws TechnicalException {
        if (item == null || item.getId() == null) {
            throw new IllegalStateException("Guardian to update must have an id");
        }
        if (internalGuardianMongoRepo.findById(item.getId()).isEmpty()) {
            throw new IllegalStateException(String.format("No guardian found with id [%s]", item.getId()));
        }
        log.debug("Update guardian with id [{}]", item.getId());
        final GuardianMongo updated = internalGuardianMongoRepo.save(mapGuardian(item));
        log.debug("Update guardian with id [{}] - Done", item.getId());
        return mapGuardian(updated);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalGuardianMongoRepo.deleteById(id);
        } catch (Exception e) {
            // no log here: the boundary logs it once, and AGENTS.md asks lower layers to wrap with
            // context and rethrow rather than duplicate the stack trace
            throw new TechnicalException("An error occurred when deleting guardian [%s]".formatted(id), e);
        }
    }

    @Override
    public Set<Guardian> findAll() throws TechnicalException {
        // Not an unfinished job: a Guardian is always read within its environment, so a global
        // findAll has no meaning here. The shared repository contract asserts this refusal.
        throw new IllegalStateException("findAll is not supported: a Guardian is always read within its environment");
    }

    @Override
    public Page<Guardian> search(GuardianCriteria criteria, Pageable pageable, Sortable sortable) throws TechnicalException {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        Objects.requireNonNull(criteria, "GuardianCriteria must not be null");
        Objects.requireNonNull(criteria.getEnvironmentId(), "EnvironmentId must not be null");
        log.debug("MongoGuardianRepository.search({}, {})", criteria, pageable);
        try {
            sortable = sortable == null ? new SortableBuilder().field("created_at").setAsc(true).build() : sortable;
            final var sortOrder = sortable.order() == Order.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
            // same allow-list as the JDBC side, from the shared criteria: without it Mongo would
            // quietly sort on a field that does not exist instead of refusing
            final var sortField = GuardianCriteria.requireSortableField(sortable.field());
            return this.internalGuardianMongoRepo.search(
                criteria,
                // _id breaks ties: a Guardian's phase takes one of two values and its deployedAt is
                // null until something deploys it, so a single sort key leaves the order undefined and
                // offset paging could repeat or skip rows between two pages.
                PageRequest.of(
                    pageable.pageNumber(),
                    pageable.pageSize(),
                    Sort.by(sortOrder, sortField).and(Sort.by(Sort.Direction.ASC, "_id"))
                )
            ).map(this::mapGuardian);
        } catch (Exception e) {
            // no log here: the boundary logs it once, and AGENTS.md asks lower layers to wrap with
            // context and rethrow rather than duplicate the stack trace
            throw new TechnicalException("An error occurred when searching for guardians", e);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete guardians by environmentId [{}]", environmentId);
        try {
            final var res = internalGuardianMongoRepo.deleteByEnvironmentId(environmentId).stream().map(GuardianMongo::getId).toList();
            log.debug("Delete guardians by environmentId [{}] - Done", environmentId);
            return res;
        } catch (Exception ex) {
            // no log here: the boundary logs it once, and AGENTS.md asks lower layers to wrap with
            // context and rethrow rather than duplicate the stack trace
            throw new TechnicalException("Failed to delete guardians by environmentId [%s]".formatted(environmentId), ex);
        }
    }

    private Guardian mapGuardian(GuardianMongo item) {
        return item == null ? null : mapper.map(item);
    }

    private GuardianMongo mapGuardian(Guardian item) {
        return item == null ? null : mapper.map(item);
    }
}
