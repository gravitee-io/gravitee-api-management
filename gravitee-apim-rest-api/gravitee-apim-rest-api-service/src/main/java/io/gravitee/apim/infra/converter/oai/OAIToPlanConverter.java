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
package io.gravitee.apim.infra.converter.oai;

import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.service.swagger.converter.extension.Metadatum;
import io.gravitee.rest.api.service.swagger.converter.extension.Plan;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OAIToPlanConverter {

    public static OAIToPlanConverter INSTANCE = new OAIToPlanConverter();

    public Set<PlanWithFlows> convert(List<Plan> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return null;
        }

        return planList
            .stream()
            .map(plan ->
                PlanWithFlows
                    .builder()
                    .flows(new ArrayList<>())
                    .name(plan.getName())
                    .validation(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.AUTO)
                    .planDefinitionHttpV4(
                        io.gravitee.definition.model.v4.plan.Plan
                            .builder()
                            .name(plan.getName())
                            .mode(PlanMode.STANDARD)
                            .security(PlanSecurity.builder().type(plan.getType().value()).build())
                            .build()
                    )
                    .build()
            )
            .collect(Collectors.toSet());
    }
}
