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
import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.model.CustomUserField;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CustomUserFieldsRespositoryProxy extends AbstractProxy<CustomUserFieldsRepository> implements CustomUserFieldsRepository {

    @Override
    public CustomUserField create(CustomUserField field) throws TechnicalException {
        return target.create(field);
    }

    @Override
    public CustomUserField update(CustomUserField field) throws TechnicalException {
        return target.update(field);
    }

    @Override
    public void delete(String key, String refId, CustomUserFieldReferenceType refType) throws TechnicalException {
        target.delete(key, refId, refType);
    }

    @Override
    public Optional<CustomUserField> findById(String key, String refId, CustomUserFieldReferenceType refType) throws TechnicalException {
        return target.findById(key, refId, refType);
    }

    @Override
    public List<CustomUserField> findByReferenceIdAndReferenceType(String refId, CustomUserFieldReferenceType refType)
        throws TechnicalException {
        return target.findByReferenceIdAndReferenceType(refId, refType);
    }
}
