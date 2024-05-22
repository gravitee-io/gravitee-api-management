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
package io.gravitee.apim.core.theme.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.infra.adapter.ThemeAdapter;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ThemeDomainService {

    private final ThemeCrudService themeCrudService;

    public Theme create(NewTheme newTheme) {
        // convert to Theme
        var themeToBeCreated = ThemeAdapter.INSTANCE.map(newTheme);

        // set id + created at + updated at
        var currentTime = ZonedDateTime.now();
        themeToBeCreated.setId(UuidString.generateRandom());
        themeToBeCreated.setCreatedAt(currentTime);
        themeToBeCreated.setUpdatedAt(currentTime);

        return this.themeCrudService.create(themeToBeCreated);
    }
}
