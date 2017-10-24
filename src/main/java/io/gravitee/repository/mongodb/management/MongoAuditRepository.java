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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.View;
import io.gravitee.repository.mongodb.management.internal.api.ViewMongoRepository;
import io.gravitee.repository.mongodb.management.internal.audit.AuditMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AuditMongo;
import io.gravitee.repository.mongodb.management.internal.model.ViewMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        Page<AuditMongo> auditsMongo = internalAuditRepo.search(filter, pageable);

        List<Audit> content = mapper.collection2list(auditsMongo.getContent(), AuditMongo.class, Audit.class);
        return new Page<>(content, auditsMongo.getPageNumber(), (int) auditsMongo.getPageElements(), auditsMongo.getTotalElements());
    }

    @Override
    public Optional<Audit> findById(String id) throws TechnicalException {
        LOGGER.debug("Find view by ID [{}]", id);

        final AuditMongo audit = internalAuditRepo.findOne(id);

        LOGGER.debug("Find view by ID [{}] - Done", id);
        return Optional.ofNullable(mapper.map(audit, Audit.class));
    }

    @Override
    public Audit create(Audit audit) throws TechnicalException {
        LOGGER.debug("Create audit {}", audit.toString());

        AuditMongo auditMongo = mapper.map(audit, AuditMongo.class);
        AuditMongo createdAuditMongo = internalAuditRepo.insert(auditMongo);

        Audit res = mapper.map(createdAuditMongo, Audit.class);

        LOGGER.debug("Create view [{}] - Done", audit.toString());

        return res;
    }


}
