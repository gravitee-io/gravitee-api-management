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
package io.gravitee.apim.core.theme.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.theme.domain_service.DefaultThemeDomainService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeSearchCriteria;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetDefaultThemeUseCase {

    private final DefaultThemeDomainService defaultThemeDomainService;

    public Output execute(Input input) {
        return new Output(this.defaultThemeDomainService.getDefaultTheme(input.type(), input.executionContext()));
    }

    @Builder
    public record Input(ThemeType type, ExecutionContext executionContext) {}

    public record Output(Theme result) {}
}
