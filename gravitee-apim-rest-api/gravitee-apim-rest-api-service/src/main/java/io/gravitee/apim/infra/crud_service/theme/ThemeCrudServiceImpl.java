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
package io.gravitee.apim.infra.crud_service.theme;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.infra.adapter.ThemeAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ThemeRepository;
import java.time.ZonedDateTime;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ThemeCrudServiceImpl implements ThemeCrudService {

    private final ThemeRepository themeRepository;

    public ThemeCrudServiceImpl(@Lazy ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    @Override
    public Theme create(Theme theme) {
        try {
            var createdTheme = themeRepository.create(ThemeAdapter.INSTANCE.map(theme));
            return ThemeAdapter.INSTANCE.map(createdTheme);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Error during theme creation", e);
        }
    }

    @Override
    public Theme update(Theme theme) {
        try {
            var updatedTheme = themeRepository.update(ThemeAdapter.INSTANCE.map(theme));
            return ThemeAdapter.INSTANCE.map(updatedTheme);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Error during theme update", e);
        }
    }

    @Override
    public Theme get(String id) {
        try {
            return themeRepository.findById(id).map(ThemeAdapter.INSTANCE::map).orElseThrow(() -> new ThemeNotFoundException(id));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Error during get", e);
        }
    }
}
