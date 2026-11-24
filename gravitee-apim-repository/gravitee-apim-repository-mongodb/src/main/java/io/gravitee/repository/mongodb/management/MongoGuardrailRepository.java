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
import io.gravitee.repository.management.api.GuardrailRepository;
import io.gravitee.repository.management.api.search.GuardrailCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Guardrail;
import io.gravitee.repository.mongodb.management.internal.guardrails.GuardrailMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.GuardrailMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Date;
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
public class MongoGuardrailRepository implements GuardrailRepository {

    private final GraviteeMapper mapper;
    private final GuardrailMongoRepository internalGuardrailMongoRepo;

    @Override
    public void updateUpdatedAt(String environmentId, String guardrailId, Date updatedAt) throws TechnicalException {
        internalGuardrailMongoRepo.updateUpdatedAt(environmentId, guardrailId, updatedAt);
    }

    @Override
    public Optional<Guardrail> findById(String id) throws TechnicalException {
        log.debug("Find guardrail by ID [{}]", id);
        final GuardrailMongo guardrailMongo = internalGuardrailMongoRepo.findById(id).orElse(null);
        log.debug("Find guardrail by ID [{}] - Done", id);
        return Optional.ofNullable(mapGuardrail(guardrailMongo));
    }

    @Override
    public List<Guardrail> findByEnvironmentIdAndIds(String environmentId, Set<String> guardrailIds) throws TechnicalException {
        Objects.requireNonNull(environmentId, "EnvironmentId must not be null");
        var ids = Set.copyOf(guardrailIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        try {
            return internalGuardrailMongoRepo.findByEnvironmentIdAndIdIn(environmentId, ids).stream().map(this::mapGuardrail).toList();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find Guardrails by environment and ids", ex);
        }
    }

    @Override
    public List<Guardrail> findByEnvironmentIdAndNameIgnoreCase(String environmentId, String name) throws TechnicalException {
        Objects.requireNonNull(environmentId, "EnvironmentId must not be null");
        Objects.requireNonNull(name, "Name must not be null");
        log.debug("Find guardrails by environment [{}] and name", environmentId);
        return internalGuardrailMongoRepo
            .findByEnvironmentIdAndNameIgnoreCase(environmentId, name)
            .stream()
            .map(this::mapGuardrail)
            .toList();
    }

    @Override
    public Guardrail create(Guardrail item) throws TechnicalException {
        log.debug("Create guardrail with id [{}]", item.getId());
        final GuardrailMongo created = internalGuardrailMongoRepo.insert(mapGuardrail(item));
        final Guardrail res = mapGuardrail(created);
        log.debug("Create guardrail with id [{}] - Done", res.getId());
        return res;
    }

    @Override
    public Guardrail update(Guardrail item) throws TechnicalException {
        if (item == null || item.getId() == null) {
            throw new IllegalStateException("Guardrail to update must have an id");
        }
        if (internalGuardrailMongoRepo.findById(item.getId()).isEmpty()) {
            throw new IllegalStateException(String.format("No guardrail found with id [%s]", item.getId()));
        }
        log.debug("Update guardrail with id [{}]", item.getId());
        final GuardrailMongo updated = internalGuardrailMongoRepo.save(mapGuardrail(item));
        log.debug("Update guardrail with id [{}] - Done", item.getId());
        return mapGuardrail(updated);
    }

    @Override
    public Optional<Guardrail> updateIfVersionMatches(Guardrail guardrail, int expectedVersion) throws TechnicalException {
        if (guardrail == null || guardrail.getId() == null) {
            throw new IllegalStateException("Guardrail to update must have an id");
        }
        log.debug("Update guardrail with id [{}] if at version [{}]", guardrail.getId(), expectedVersion);
        try {
            return internalGuardrailMongoRepo.replaceIfVersionMatches(mapGuardrail(guardrail), expectedVersion).map(this::mapGuardrail);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when updating guardrail [%s]".formatted(guardrail.getId()), e);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalGuardrailMongoRepo.deleteById(id);
        } catch (Exception e) {
            // no log here: the boundary logs it once, and AGENTS.md asks lower layers to wrap with
            // context and rethrow rather than duplicate the stack trace
            throw new TechnicalException("An error occurred when deleting guardrail [%s]".formatted(id), e);
        }
    }

    @Override
    public Set<Guardrail> findAll() throws TechnicalException {
        // Not an unfinished job: a Guardrail is always read within its environment, so a global
        // findAll has no meaning here. The shared repository contract asserts this refusal.
        throw new IllegalStateException("findAll is not supported: a Guardrail is always read within its environment");
    }

    @Override
    public Page<Guardrail> search(GuardrailCriteria criteria, Pageable pageable, Sortable sortable) throws TechnicalException {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        Objects.requireNonNull(criteria, "GuardrailCriteria must not be null");
        Objects.requireNonNull(criteria.getEnvironmentId(), "EnvironmentId must not be null");
        log.debug("MongoGuardrailRepository.search({}, {})", criteria, pageable);
        try {
            sortable = sortable == null ? new SortableBuilder().field("created_at").setAsc(true).build() : sortable;
            final var sortOrder = sortable.order() == Order.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
            // same allow-list as the JDBC side, from the shared criteria: without it Mongo would
            // quietly sort on a field that does not exist instead of refusing
            final var sortField = GuardrailCriteria.requireSortableField(sortable.field());
            return this.internalGuardrailMongoRepo.search(
                criteria,
                // _id breaks ties: a Guardrail's phase takes one of two values and its deployedAt is
                // null until something deploys it, so a single sort key leaves the order undefined and
                // offset paging could repeat or skip rows between two pages.
                PageRequest.of(
                    pageable.pageNumber(),
                    pageable.pageSize(),
                    Sort.by(sortOrder, sortField).and(Sort.by(Sort.Direction.ASC, "_id"))
                )
            ).map(this::mapGuardrail);
        } catch (Exception e) {
            // no log here: the boundary logs it once, and AGENTS.md asks lower layers to wrap with
            // context and rethrow rather than duplicate the stack trace
            throw new TechnicalException("An error occurred when searching for guardrails", e);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete guardrails by environmentId [{}]", environmentId);
        try {
            final var res = internalGuardrailMongoRepo.deleteByEnvironmentId(environmentId).stream().map(GuardrailMongo::getId).toList();
            log.debug("Delete guardrails by environmentId [{}] - Done", environmentId);
            return res;
        } catch (Exception ex) {
            // no log here: the boundary logs it once, and AGENTS.md asks lower layers to wrap with
            // context and rethrow rather than duplicate the stack trace
            throw new TechnicalException("Failed to delete guardrails by environmentId [%s]".formatted(environmentId), ex);
        }
    }

    private Guardrail mapGuardrail(GuardrailMongo item) {
        return item == null ? null : mapper.map(item);
    }

    private GuardrailMongo mapGuardrail(Guardrail item) {
        return item == null ? null : mapper.map(item);
    }
}
