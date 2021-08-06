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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ParameterRepositoryProxy extends AbstractProxy<ParameterRepository> implements ParameterRepository {

    @Override
    public Optional<Parameter> findById(String key, String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        return target.findById(key, referenceId, referenceType);
    }

    @Override
    public List<Parameter> findByKeys(List<String> keys, String referenceId, ParameterReferenceType referenceType)
        throws TechnicalException {
        return target.findByKeys(keys, referenceId, referenceType);
    }

    @Override
    public Parameter create(Parameter item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Parameter update(Parameter item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public void delete(String key, String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        target.delete(key, referenceId, referenceType);
    }

    @Override
    public List<Parameter> findAll(String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        return target.findAll(referenceId, referenceType);
    }
}
