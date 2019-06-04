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
package io.gravitee.rest.api.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MetadataRepositoryProxy extends AbstractProxy<MetadataRepository> implements MetadataRepository {

    @Override
    public Optional<Metadata> findById(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException {
        return target.findById(key, referenceId, referenceType);
    }

    @Override
    public Metadata create(Metadata item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Metadata update(Metadata item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public void delete(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException {
        target.delete(key, referenceId, referenceType);
    }

    @Override
    public List<Metadata> findByReferenceType(MetadataReferenceType referenceType) throws TechnicalException {
        return target.findByReferenceType(referenceType);
    }

    @Override
    public List<Metadata> findByReferenceTypeAndReferenceId(MetadataReferenceType referenceType, String referenceId) throws TechnicalException {
        return target.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    @Override
    public List<Metadata> findByKeyAndReferenceType(String key, MetadataReferenceType referenceType) throws TechnicalException {
        return target.findByKeyAndReferenceType(key, referenceType);
    }
}
