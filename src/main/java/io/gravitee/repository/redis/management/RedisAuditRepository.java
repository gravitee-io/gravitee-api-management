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
package io.gravitee.repository.redis.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.redis.management.internal.AuditRedisRepository;
import io.gravitee.repository.redis.management.model.RedisAudit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisAuditRepository implements AuditRepository {

    @Autowired
    private AuditRedisRepository auditRedisRepository;

    @Override
    public Optional<Audit> findById(String id) throws TechnicalException {
        RedisAudit redisAudit = this.auditRedisRepository.find(id);
        return Optional.ofNullable(convert(redisAudit));
    }

    @Override
    public Audit create(Audit audit) throws TechnicalException {
        RedisAudit redisAudit = auditRedisRepository.saveOrUpdate(convert(audit));
        return convert(redisAudit);
    }

    @Override
    public Page<Audit> search(AuditCriteria filter, Pageable pageable) {
        Page<RedisAudit> pagedEvents = auditRedisRepository.search(filter, pageable);

        return new Page<>(
                pagedEvents.getContent().stream().map(this::convert).collect(Collectors.toList()),
                pagedEvents.getPageNumber(), (int) pagedEvents.getPageElements(),
                pagedEvents.getTotalElements());
    }

    private Audit convert(RedisAudit redisAudit) {
        if (redisAudit == null) {
            return null;
        }

        Audit audit = new Audit();

        audit.setId(redisAudit.getId());
        audit.setProperties(redisAudit.getProperties());
        audit.setEvent(redisAudit.getEvent());
        audit.setPatch(redisAudit.getPatch());
        audit.setReferenceId(redisAudit.getReferenceId());
        audit.setUsername(redisAudit.getUsername());
        audit.setReferenceType(Audit.AuditReferenceType.valueOf(redisAudit.getReferenceType()));
        audit.setCreatedAt(new Date(redisAudit.getCreatedAt()));

        return audit;
    }

    private RedisAudit convert(Audit audit) {
        RedisAudit redisAudit = new RedisAudit();

        redisAudit.setId(audit.getId());
        redisAudit.setProperties(audit.getProperties());
        redisAudit.setEvent(audit.getEvent());
        redisAudit.setPatch(audit.getPatch());
        redisAudit.setReferenceId(audit.getReferenceId());
        redisAudit.setUsername(audit.getUsername());
        redisAudit.setReferenceType(audit.getReferenceType().name());
        redisAudit.setCreatedAt(audit.getCreatedAt().getTime());

        return redisAudit;
    }
}
