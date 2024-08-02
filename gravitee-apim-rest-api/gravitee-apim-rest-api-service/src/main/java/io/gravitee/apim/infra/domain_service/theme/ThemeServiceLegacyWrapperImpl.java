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
package io.gravitee.apim.infra.domain_service.theme;

import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.theme.domain_service.ThemeServiceLegacyWrapper;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.PrimaryOwnerAdapter;
import io.gravitee.apim.infra.adapter.ThemeAdapter;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import org.springframework.stereotype.Service;

@Service
public class ThemeServiceLegacyWrapperImpl implements ThemeServiceLegacyWrapper {

    private final ThemeService themeService;

    public ThemeServiceLegacyWrapperImpl(ThemeService themeService) {
        this.themeService = themeService;
    }

    @Override
    public Theme getCurrentOrCreateDefaultPortalTheme(ExecutionContext executionContext) {
        var theme = this.themeService.findOrCreateDefaultPortalTheme(executionContext);
        return ThemeAdapter.INSTANCE.map(theme);
    }
}
