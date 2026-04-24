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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.KafkaPortRangeRepository;
import io.gravitee.repository.management.model.KafkaPortRange;
import io.gravitee.repository.mongodb.management.internal.kafkaportranges.KafkaPortRangeMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.KafkaPortRangeMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@AllArgsConstructor
public class MongoKafkaPortRangeRepository implements KafkaPortRangeRepository {

    private final KafkaPortRangeMongoRepository internalRepository;
    private final GraviteeMapper mapper;

    @Override
    public Optional<KafkaPortRange> findById(String planId) throws TechnicalException {
        log.debug("Find kafka port range by plan id [{}]", planId);
        return internalRepository.findById(planId).map(mapper::map);
    }

    @Override
    public Set<KafkaPortRange> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(mapper::map).collect(Collectors.toSet());
    }

    @Override
    public KafkaPortRange create(KafkaPortRange item) throws TechnicalException {
        log.debug("Create kafka port range for plan [{}]", item.getPlanId());
        KafkaPortRangeMongo saved = internalRepository.insert(mapper.map(item));
        return mapper.map(saved);
    }

    @Override
    public KafkaPortRange update(KafkaPortRange item) throws TechnicalException {
        log.debug("Update kafka port range for plan [{}]", item.getPlanId());
        if (!internalRepository.existsById(item.getPlanId())) {
            throw new IllegalStateException(String.format("No kafka port range found for plan [%s]", item.getPlanId()));
        }
        KafkaPortRangeMongo saved = internalRepository.save(mapper.map(item));
        return mapper.map(saved);
    }

    @Override
    public void delete(String planId) throws TechnicalException {
        log.debug("Delete kafka port range for plan [{}]", planId);
        internalRepository.deleteById(planId);
    }

    @Override
    public List<KafkaPortRange> findConflicting(
        String environmentId,
        String shardingTag,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) throws TechnicalException {
        return internalRepository
            .findConflicting(environmentId, shardingTag, bootstrapPort, rangeStart, rangeEnd, excludePlanId)
            .stream()
            .map(mapper::map)
            .toList();
    }

    @Override
    public List<KafkaPortRange> findConflictingForUpdate(
        String environmentId,
        String shardingTag,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) throws TechnicalException {
        // MongoDB row-level locks require a multi-document transaction on a replica set or sharded
        // cluster plus a ClientSession passed on every operation. Without that wiring, SELECT ...
        // FOR UPDATE has no MongoDB equivalent, so we fall back to the non-locking query. Deployments
        // that require strict concurrent-save protection on MongoDB should enable multi-document
        // transactions and route this call through a ClientSession.
        return findConflicting(environmentId, shardingTag, bootstrapPort, rangeStart, rangeEnd, excludePlanId);
    }

    @Override
    public void deleteByApiId(String apiId) throws TechnicalException {
        log.debug("Delete kafka port ranges for api [{}]", apiId);
        internalRepository.deleteByApiId(apiId);
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete kafka port ranges for environment [{}]", environmentId);
        internalRepository.deleteByEnvironmentId(environmentId);
    }
}
