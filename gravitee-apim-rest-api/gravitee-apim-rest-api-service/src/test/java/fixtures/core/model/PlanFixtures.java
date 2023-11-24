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

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class PlanFixtures {

    private PlanFixtures() {}

    private static final Supplier<Plan.PlanBuilder> BASE = () ->
        Plan
            .builder()
            .id("my-plan")
            .apiId("my-api")
            .name("My plan")
            .description("Description")
            .order(1)
            .characteristics(List.of("characteristic1", "characteristic2"))
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .needRedeployAt(Date.from(Instant.parse("2051-02-01T20:22:02.00Z")))
            .commentMessage("Comment message")
            .crossId("my-plan-crossId")
            .generalConditions("General conditions")
            .tags(Set.of("tag1", "tag2"))
            .status(PlanStatus.PUBLISHED)
            .security(PlanSecurity.builder().type(PlanSecurityType.KEY_LESS.getLabel()).configuration("{\"nice\": \"config\"}").build())
            .type(Plan.PlanType.API)
            .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
            .validation(Plan.PlanValidationType.AUTO)
            .selectionRule("{#request.attribute['selectionRule'] != null}");

    public static Plan aPlanV4() {
        return BASE.get().build();
    }

    public static Plan aPlanV2() {
        return BASE.get().paths(Map.of("/", List.of())).build();
    }
}
