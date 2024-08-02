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
package io.gravitee.apim.infra.query_service.theme;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeSearchCriteria;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import io.gravitee.apim.infra.adapter.ThemeAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.api.search.ThemeCriteria;
import io.gravitee.repository.management.model.ThemeType;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ThemeQueryServiceImpl extends AbstractService implements ThemeQueryService {

    private final ThemeRepository themeRepository;

    public ThemeQueryServiceImpl(@Lazy final ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    @Override
    public Page<Theme> searchByCriteria(ThemeSearchCriteria criteria, Pageable pageable) {
        var type = criteria.getType() == null ? null : ThemeType.valueOf(criteria.getType().name());
        try {
            var page =
                this.themeRepository.search(ThemeCriteria.builder().enabled(criteria.getEnabled()).type(type).build(), convert(pageable));

            return page.map(ThemeAdapter.INSTANCE::map);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }

    @Override
    public Set<Theme> findByThemeTypeAndEnvironmentId(io.gravitee.apim.core.theme.model.ThemeType themeType, String environmentId) {
        try {
            return ThemeAdapter.INSTANCE.map(
                this.themeRepository.findByReferenceIdAndReferenceTypeAndType(
                        environmentId,
                        Theme.ReferenceType.ENVIRONMENT.name(),
                        ThemeType.valueOf(themeType.name())
                    )
            );
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }
}
