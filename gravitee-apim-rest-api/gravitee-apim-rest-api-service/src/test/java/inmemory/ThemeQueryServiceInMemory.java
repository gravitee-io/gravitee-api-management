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
package inmemory;

import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeSearchCriteria;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ThemeQueryServiceInMemory implements ThemeQueryService, InMemoryAlternative<Theme> {

    private final List<Theme> storage;

    public ThemeQueryServiceInMemory() {
        this.storage = new ArrayList<>();
    }

    public ThemeQueryServiceInMemory(ThemeCrudServiceInMemory themeCrudServiceInMemory) {
        storage = themeCrudServiceInMemory.storage;
    }

    @Override
    public void initWith(List<Theme> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Theme> storage() {
        return Collections.unmodifiableList(this.storage);
    }

    @Override
    public Page<Theme> searchByCriteria(ThemeSearchCriteria criteria, Pageable pageable) {
        var themes = storage
            .stream()
            .filter(theme ->
                (Objects.isNull(criteria.getType()) || criteria.getType().equals(theme.getType())) &&
                (Objects.isNull(criteria.getEnabled()) || criteria.getEnabled().equals(theme.isEnabled()))
            )
            .toList();

        return new Page<>(themes, 1, themes.size(), themes.size());
    }

    @Override
    public Set<Theme> findByThemeTypeAndEnvironmentId(ThemeType themeType, String environmentId) {
        return storage
            .stream()
            .filter(theme ->
                Objects.equals(themeType, theme.getType()) &&
                Objects.equals(environmentId, theme.getReferenceId()) &&
                Objects.equals(Theme.ReferenceType.ENVIRONMENT, theme.getReferenceType())
            )
            .collect(Collectors.toSet());
    }
}
