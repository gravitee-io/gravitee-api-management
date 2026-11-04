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
package fixtures;

import io.gravitee.apim.core.guardian.model.GuardianLifecycleState;
import io.gravitee.apim.core.guardian.model.GuardianPhase;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.CreateGuardian;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGuardian;
import java.time.ZonedDateTime;

/**
 * @author GraviteeSource Team
 */
public class GuardianFixtures {

    private GuardianFixtures() {}

    public static io.gravitee.apim.core.guardian.model.Guardian aGuardian() {
        return io.gravitee.apim.core.guardian.model.Guardian.builder()
            .id("guardian-id")
            .environmentId("my-env")
            .name("Credential leakage")
            .description("Blocks a prompt that carries a credential")
            .version("1.0.0")
            .apiType(ApiType.LLM_PROXY)
            .phase(GuardianPhase.REQUEST)
            .definition("{\"policies\":[]}")
            .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
            .createdAt(ZonedDateTime.parse("2020-02-01T20:22:02.00Z"))
            .updatedAt(ZonedDateTime.parse("2020-02-02T20:22:02.00Z"))
            .build();
    }

    public static CreateGuardian aCreateGuardian() {
        return new CreateGuardian()
            .name("Credential leakage")
            .description("Blocks a prompt that carries a credential")
            .version("1.0.0")
            .apiType(io.gravitee.rest.api.management.v2.rest.model.ApiType.LLM_PROXY)
            .phase(io.gravitee.rest.api.management.v2.rest.model.GuardianPhase.REQUEST);
    }

    public static UpdateGuardian anUpdateGuardian() {
        return new UpdateGuardian().name("Renamed").description("New description").version("2.0.0");
    }
}
