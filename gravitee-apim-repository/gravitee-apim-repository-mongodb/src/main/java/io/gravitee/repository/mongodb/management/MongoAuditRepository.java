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
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.mongodb.management.internal.audit.AuditMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AuditMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoAuditRepository implements AuditRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoAuditRepository.class);

    @Autowired
    private AuditMongoRepository internalAuditRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Page<Audit> search(AuditCriteria filter, Pageable pageable) {
        return internalAuditRepo.search(filter, pageable).map(mapper::map);
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, Audit.AuditReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Delete audit by reference [{}/{}]", referenceId, referenceType);

        final var audits = internalAuditRepo
            .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
            .stream()
            .map(AuditMongo::getId)
            .toList();

        LOGGER.debug("Delete audit by reference [{}/{}] - Done", referenceId, referenceType);
        return audits;
    }

    @Override
    public Optional<Audit> findById(String id) throws TechnicalException {
        LOGGER.debug("Find audit by ID [{}]", id);

        final AuditMongo audit = internalAuditRepo.findById(id).orElse(null);

        LOGGER.debug("Find audit by ID [{}] - Done", id);
        return Optional.ofNullable(mapper.map(audit));
    }

    @Override
    public Audit create(Audit audit) throws TechnicalException {
        LOGGER.debug("Create audit {}", audit.toString());

        AuditMongo auditMongo = mapper.map(audit);
        AuditMongo createdAuditMongo = internalAuditRepo.insert(auditMongo);

        Audit res = mapper.map(createdAuditMongo);

        LOGGER.debug("Create audit [{}] - Done", audit);

        return res;
    }

    @Override
    public Set<Audit> findAll() throws TechnicalException {
        throw new IllegalStateException("not implemented cause of high amount of data. Use pageable search instead");
    }
}
