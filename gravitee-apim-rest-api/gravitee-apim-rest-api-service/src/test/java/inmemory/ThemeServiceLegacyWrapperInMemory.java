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

import io.gravitee.apim.core.theme.domain_service.ThemeServiceLegacyWrapper;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ThemeServiceLegacyWrapperInMemory implements ThemeServiceLegacyWrapper, InMemoryAlternative<Theme> {

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
    public Theme getCurrentOrCreateDefaultPortalTheme(ExecutionContext executionContext) {
        return storage
            .stream()
            .filter(theme ->
                Objects.equals(ThemeType.PORTAL, theme.getType()) &&
                Objects.equals(true, theme.isEnabled()) &&
                Theme.ReferenceType.ENVIRONMENT.equals(theme.getReferenceType()) &&
                Objects.equals(executionContext.getEnvironmentId(), theme.getReferenceId())
            )
            .findFirst()
            .orElseGet(() ->
                Theme
                    .builder()
                    .id("portal-default-theme")
                    .type(ThemeType.PORTAL)
                    .referenceId(executionContext.getEnvironmentId())
                    .referenceType(Theme.ReferenceType.ENVIRONMENT)
                    .enabled(true)
                    .definitionPortal(new ThemeDefinition())
                    .build()
            );
    }
}
