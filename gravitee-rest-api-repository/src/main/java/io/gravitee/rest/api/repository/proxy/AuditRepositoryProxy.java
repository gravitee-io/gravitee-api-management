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
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Audit;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AuditRepositoryProxy extends AbstractProxy<AuditRepository> implements AuditRepository {

    @Override
    public Audit create(Audit audit) throws TechnicalException {
        return target.create(audit);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }

    @Override
    public Optional<Audit> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public Audit update(Audit audit) throws TechnicalException {
        return target.update(audit);
    }

    @Override
    public Page<Audit> search(AuditCriteria filter, Pageable pageable) {
        return target.search(filter, pageable);
    }
}
