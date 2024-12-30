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
package fixtures.core.model;

import fixtures.definition.PlanFixtures;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class PlanWithFlowsFixtures {

    private PlanWithFlowsFixtures() {}

    private static final Supplier<PlanWithFlows.PlanWithFlowsBuilder> BASE = () ->
        PlanWithFlows
            .builder()
            .id("id")
            .crossId("my-plan-crossId")
            .name("My plan")
            .description("Description")
            .validation(Plan.PlanValidationType.AUTO)
            .type(Plan.PlanType.API)
            .planDefinitionHttpV4(
                PlanFixtures.HttpV4Definition
                    .anApiKeyV4()
                    .toBuilder()
                    .security(
                        PlanSecurity
                            .builder()
                            .type("API_KEY")
                            .configuration("""
                           {"nice": "config"}
                           """)
                            .build()
                    )
                    .selectionRule("{#request.attribute['selectionRule'] != null}")
                    .tags(Set.of("tag1", "tag2"))
                    .status(PlanStatus.CLOSED)
                    .build()
            )
            .apiId("api-id")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .commentMessage("Comment message")
            .commentRequired(true)
            .generalConditions("General conditions")
            .flows(List.of(fixtures.definition.FlowFixtures.aSimpleFlowV4()));

    public static PlanWithFlows aPlanWithFlows() {
        return BASE.get().build();
    }
}
