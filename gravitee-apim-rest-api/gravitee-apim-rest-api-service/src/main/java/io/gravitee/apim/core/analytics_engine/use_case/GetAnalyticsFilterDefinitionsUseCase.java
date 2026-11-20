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
package io.gravitee.apim.core.analytics_engine.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.domain_service.EndUserDisplaySettingDomainService;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.observability.model.Signal;
import java.util.List;
import java.util.Set;

@UseCase
public class GetAnalyticsFilterDefinitionsUseCase {

    private final AnalyticsDefinitionQueryService definition;
    private final EndUserDisplaySettingDomainService endUserDisplaySetting;

    public GetAnalyticsFilterDefinitionsUseCase(
        AnalyticsDefinitionQueryService definition,
        EndUserDisplaySettingDomainService endUserDisplaySetting
    ) {
        this.definition = definition;
        this.endUserDisplaySetting = endUserDisplaySetting;
    }

    /**
     * @param organizationId the organization whose settings decide whether the end user is offered
     * @param signals signals to narrow the catalog to; empty returns the whole catalog.
     */
    public record Input(String organizationId, Set<Signal> signals) {}

    public record Output(List<FilterSpec> specs) {}

    public Output execute(Input input) {
        var specs = definition.getFilters(input.signals());
        if (endUserDisplaySetting.isDisplayed(input.organizationId())) {
            return new Output(specs);
        }
        return new Output(
            specs
                .stream()
                .filter(spec -> spec.name() != FilterSpec.Name.USER)
                .toList()
        );
    }
}
