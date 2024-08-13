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
import io.gravitee.repository.management.api.SharedPolicyGroupHistoryRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SharedPolicyGroupHistoryCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.SharedPolicyGroup;
import io.gravitee.repository.mongodb.management.internal.model.SharedPolicyGroupHistoryMongo;
import io.gravitee.repository.mongodb.management.internal.model.SharedPolicyGroupMongo;
import io.gravitee.repository.mongodb.management.internal.sharedpolicygrouphistory.SharedPolicyGroupHistoryMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoSharedPolicyGroupHistoryRepository implements SharedPolicyGroupHistoryRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoSharedPolicyGroupHistoryRepository.class);

    private final GraviteeMapper mapper;

    private final SharedPolicyGroupHistoryMongoRepository internalSharedPolicyGroupHistoryMongoRepo;

    @Override
    public Page<SharedPolicyGroup> search(SharedPolicyGroupHistoryCriteria criteria, Pageable pageable, Sortable sortable)
        throws TechnicalException {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        Objects.requireNonNull(criteria, "SharedPolicyGroupCriteria must not be null");
        Objects.requireNonNull(criteria.getEnvironmentId(), "EnvironmentId must not be null");
        LOGGER.debug("MongoSharedPolicyGroupHistoryRepository.search({}, {})", criteria.toString(), pageable.toString());

        try {
            sortable = sortable == null ? new SortableBuilder().field("updated_at").setAsc(true).build() : sortable;
            final var sortOrder = sortable.order() == Order.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
            final var sortField = FieldUtils.toCamelCase(sortable.field());

            return this.internalSharedPolicyGroupHistoryMongoRepo.search(
                    criteria,
                    PageRequest.of(pageable.pageNumber(), pageable.pageSize(), sortOrder, sortField)
                )
                .map(this::mapSharedPolicyGroupHistory);
        } catch (Exception e) {
            LOGGER.error("An error occurred when searching for shared policy group history", e);
            throw new TechnicalException("An error occurred when searching for shared policy group history", e);
        }
    }

    @Override
    public Page<SharedPolicyGroup> searchLatestBySharedPolicyGroupId(String environmentId, Pageable pageable) throws TechnicalException {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        Objects.requireNonNull(environmentId, "EnvironmentId must not be null");
        LOGGER.debug(
            "MongoSharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyGroupId({}, {})",
            environmentId,
            pageable.toString()
        );

        try {
            return this.internalSharedPolicyGroupHistoryMongoRepo.searchLatestBySharedPolicyGroupId(
                    environmentId,
                    pageable.pageNumber(),
                    pageable.pageSize()
                )
                .map(this::mapSharedPolicyGroupHistory);
        } catch (Exception e) {
            LOGGER.error("An error occurred when searching for shared policy group history", e);
            throw new TechnicalException("An error occurred when searching for shared policy group history", e);
        }
    }

    @Override
    public Optional<SharedPolicyGroup> getLatestBySharedPolicyGroupId(String environmentId, String sharedPolicyGroupId)
        throws TechnicalException {
        LOGGER.debug(
            "Get latest shared policy group by environment ID [{}] and shared policy group ID [{}]",
            environmentId,
            sharedPolicyGroupId
        );

        final SharedPolicyGroupHistoryMongo sharedPolicyGroupMongo = internalSharedPolicyGroupHistoryMongoRepo
            .getLatestBySharedPolicyGroupId(environmentId, sharedPolicyGroupId)
            .orElse(null);

        LOGGER.debug(
            "Get shared policy group by environment ID [{}] and shared policy group ID [{}] - Done",
            environmentId,
            sharedPolicyGroupId
        );

        return Optional.ofNullable(mapSharedPolicyGroupHistory(sharedPolicyGroupMongo));
    }

    @Override
    public Optional<SharedPolicyGroup> findById(String s) throws TechnicalException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public SharedPolicyGroup create(SharedPolicyGroup item) throws TechnicalException {
        LOGGER.debug("Create shared policy group history with id [{}]", item.getId());

        SharedPolicyGroupHistoryMongo sharedPolicyGroupHistoryMongo = mapSharedPolicyGroup(item);

        SharedPolicyGroupHistoryMongo created = internalSharedPolicyGroupHistoryMongoRepo.insert(sharedPolicyGroupHistoryMongo);

        SharedPolicyGroup res = mapSharedPolicyGroupHistory(created);

        LOGGER.debug("Create shared policy group history with id [{}] - Done", res.getId());
        return res;
    }

    @Override
    public SharedPolicyGroup update(SharedPolicyGroup item) throws TechnicalException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalSharedPolicyGroupHistoryMongoRepo.deleteBySharedPolicyGroupId(id);
        } catch (Exception e) {
            LOGGER.error("An error occurred when deleting shared policy group history [{}]", id, e);
            throw new TechnicalException("An error occurred when deleting shared policy group history");
        }
    }

    @Override
    public Set<SharedPolicyGroup> findAll() throws TechnicalException {
        throw new IllegalStateException("Not implemented");
    }

    private SharedPolicyGroup mapSharedPolicyGroupHistory(SharedPolicyGroupHistoryMongo item) {
        return item == null ? null : mapper.mapHistory(item);
    }

    private SharedPolicyGroupHistoryMongo mapSharedPolicyGroup(SharedPolicyGroup item) {
        return item == null ? null : mapper.mapHistory(item);
    }
}
