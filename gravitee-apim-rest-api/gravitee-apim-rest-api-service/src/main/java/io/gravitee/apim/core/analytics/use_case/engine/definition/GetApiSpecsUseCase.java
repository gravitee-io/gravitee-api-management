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
package io.gravitee.apim.core.analytics.use_case.engine.definition;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.domain_service.engine.definition.AnalyticsDefinition;
import io.gravitee.apim.core.analytics.model.engine.definition.ApiSpec;
import java.util.List;

@UseCase
public class GetApiSpecsUseCase {

    private final AnalyticsDefinition definitionDomainService;

    public GetApiSpecsUseCase(AnalyticsDefinition definitionDomainService) {
        this.definitionDomainService = definitionDomainService;
    }

    public record Output(List<ApiSpec> specs) {}

    public Output execute() {
        return new Output(definitionDomainService.getApis());
    }
}
