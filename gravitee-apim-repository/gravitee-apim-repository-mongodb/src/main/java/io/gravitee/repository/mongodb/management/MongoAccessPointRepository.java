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

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.management.model.AccessPointTarget;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.mongodb.management.internal.domain.AccessPointMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AccessPointMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class MongoAccessPointRepository implements AccessPointRepository {

    @Autowired
    private AccessPointMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Set<AccessPoint> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public Optional<AccessPoint> findById(String id) throws TechnicalException {
        log.debug("Find access point by ID [{}]", id);

        AccessPointMongo accessPointMongo = internalRepository.findById(id).orElse(null);
        AccessPoint res = map(accessPointMongo);

        log.debug("Find access point by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public Optional<AccessPoint> findByHost(final String host) {
        log.debug("Find access point by host [{}]", host);
        final AccessPointMongo accessPointMongo = internalRepository.findByHostAndStatus(host, AccessPointStatus.CREATED);
        AccessPoint res = map(accessPointMongo);
        log.debug("Find access point by host value [{}] - Done", host);
        return Optional.ofNullable(res);
    }

    @Override
    public List<AccessPoint> findByReferenceAndTarget(
        AccessPointReferenceType referenceType,
        String referenceId,
        AccessPointTarget target
    ) {
        final List<AccessPointMongo> accessPointMongos = internalRepository.findAllByReferenceAndTargetAndStatus(
            referenceType.name(),
            referenceId,
            target.name(),
            AccessPointStatus.CREATED
        );
        return accessPointMongos.stream().map(this::map).toList();
    }

    @Override
    public List<AccessPoint> findByCriteria(AccessPointCriteria criteria, Long page, Long size) {
        List<AccessPointMongo> accessPointMongos = internalRepository.search(criteria, page, size);
        return accessPointMongos.stream().map(this::map).toList();
    }

    @Override
    public List<AccessPoint> findByTarget(final AccessPointTarget target) {
        final List<AccessPointMongo> accessPointMongos = internalRepository.findAllByTargetAndStatus(
            target.name(),
            AccessPointStatus.CREATED
        );
        return accessPointMongos.stream().map(this::map).toList();
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(final String referenceId, final AccessPointReferenceType referenceType)
        throws TechnicalException {
        log.debug("Delete access point by reference [{}, {}]", referenceType, referenceId);
        try {
            List<AccessPointMongo> accessPointMongos = internalRepository.deleteAllByReference(referenceType.name(), referenceId);
            log.debug("Delete access point by reference [{}, {}] - Done", referenceType, referenceId);
            return accessPointMongos.stream().map(AccessPointMongo::getId).toList();
        } catch (Exception e) {
            throw new TechnicalException("An error occurred while deleting access point", e);
        }
    }

    @Override
    public AccessPoint create(final AccessPoint accessPoint) throws TechnicalException {
        log.debug("Create access point [{}]", accessPoint);
        AccessPoint accessPointCreated = map(internalRepository.insert(map(accessPoint)));
        log.debug("Create access point [{}] - Done", accessPointCreated.getId());
        return accessPointCreated;
    }

    @Override
    public AccessPoint update(final AccessPoint accessPoint) throws TechnicalException {
        if (accessPoint == null) {
            throw new IllegalStateException("Access point must not be null");
        }

        internalRepository
            .findById(accessPoint.getId())
            .orElseThrow(() -> new IllegalStateException(String.format("No access point found with id [%s]", accessPoint.getId())));

        log.debug("Update access point [{}]", accessPoint);
        return map(internalRepository.save(map(accessPoint)));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete access point [{}]", id);
        internalRepository.deleteById(id);
        log.debug("Delete access point [{}] - Done", id);
    }

    private AccessPointMongo map(AccessPoint accessPoint) {
        return mapper.map(accessPoint);
    }

    private AccessPoint map(AccessPointMongo accessPointMongo) {
        return mapper.map(accessPointMongo);
    }
}
