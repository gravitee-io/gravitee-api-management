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
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Cluster;
import io.gravitee.repository.mongodb.management.internal.clusters.ClusterMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ClusterMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import io.gravitee.repository.mongodb.utils.FieldUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class MongoClusterRepository implements ClusterRepository {

    private final ClusterMongoRepository internalClusterMongoRepo;
    private final GraviteeMapper mapper;

    @Override
    public Optional<Cluster> findById(String id) throws TechnicalException {
        log.debug("Find cluster by ID [{}]", id);
        final ClusterMongo cluster = internalClusterMongoRepo.findById(id).orElse(null);
        log.debug("Find cluster by ID [{}] - Done", id);
        return Optional.ofNullable(mapper.map(cluster));
    }

    @Override
    public Cluster create(Cluster cluster) throws TechnicalException {
        log.debug("Create cluster with id [{}]", cluster.getId());

        ClusterMongo clusterMongo = mapper.map(cluster);
        clusterMongo = internalClusterMongoRepo.insert(clusterMongo);
        Cluster createdCluster = mapper.map(clusterMongo);

        log.debug("Create cluster with id [{}] - Done", createdCluster.getId());

        return createdCluster;
    }

    @Override
    public Cluster update(Cluster cluster) throws TechnicalException {
        log.debug("Update cluster with id [{}]", cluster.getId());

        if (!internalClusterMongoRepo.existsById(cluster.getId())) {
            throw new IllegalStateException(String.format("No cluster found with id [%s]", cluster.getId()));
        }

        ClusterMongo clusterMongo = mapper.map(cluster);
        clusterMongo = internalClusterMongoRepo.save(clusterMongo);

        log.debug("Update cluster with id [{}] - Done", cluster.getId());

        return mapper.map(clusterMongo);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete cluster with id [{}]", id);
        internalClusterMongoRepo.deleteById(id);
    }

    @Override
    public Set<Cluster> findAll() throws TechnicalException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Page<Cluster> search(ClusterCriteria criteria, Pageable pageable, Optional<Sortable> sortableOpt) {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        Objects.requireNonNull(criteria, "ClusterCriteria must not be null");
        Objects.requireNonNull(criteria.getEnvironmentId(), "ClusterCriteria.getEnvironmentId() must not be null");
        log.debug("MongoClusterRepository.search({}, {})", criteria, pageable);

        Sortable sortable = sortableOpt.orElse(new SortableBuilder().field("name").setAsc(true).build());
        final Sort.Direction sortOrder = Sort.Direction.valueOf(sortable.order().name());
        final String sortField = FieldUtils.toCamelCase(sortable.field());

        return this.internalClusterMongoRepo.search(
                criteria,
                PageRequest.of(pageable.pageNumber(), pageable.pageSize(), sortOrder, sortField)
            )
            .map(mapper::map);
    }
}
