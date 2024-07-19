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

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.model.Theme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public class ThemeCrudServiceInMemory implements ThemeCrudService, InMemoryAlternative<Theme> {

    final List<Theme> storage = new ArrayList<>();

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
    public Theme create(Theme theme) {
        this.storage.add(theme);
        return theme;
    }

    @Override
    public Theme get(String id) {
        return storage
            .stream()
            .filter(theme -> Objects.equals(id, theme.getId()))
            .findAny()
            .orElseThrow(() -> new ThemeNotFoundException(id));
    }
}
