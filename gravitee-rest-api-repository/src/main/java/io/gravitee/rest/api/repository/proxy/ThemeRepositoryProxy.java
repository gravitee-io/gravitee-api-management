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
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.Theme;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ThemeRepositoryProxy extends AbstractProxy<ThemeRepository> implements ThemeRepository {

    @Override
    public Set<Theme> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<Theme> findByReferenceIdAndReferenceType(String referenceId, String referenceType) throws TechnicalException {
        return target.findByReferenceIdAndReferenceType(referenceId, referenceType);
    }

    @Override
    public Optional<Theme> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public Theme create(Theme theme) throws TechnicalException {
        return target.create(theme);
    }

    @Override
    public Theme update(Theme theme) throws TechnicalException {
        return target.update(theme);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }
}
