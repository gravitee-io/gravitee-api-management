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

import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD;
import io.gravitee.common.utils.UUID;
import io.gravitee.rest.api.management.v2.rest.model.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.CreateSharedPolicyGroup;
import io.gravitee.rest.api.management.v2.rest.model.FlowPhase;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSharedPolicyGroup;
import java.util.function.Supplier;

public class SharedPolicyGroupFixtures {

    private SharedPolicyGroupFixtures() {}

    private static final Supplier<CreateSharedPolicyGroup> CREATE_BASE = () ->
        new CreateSharedPolicyGroup().name("My Shared Policy Group").apiType(ApiType.PROXY).phase(FlowPhase.REQUEST);

    public static CreateSharedPolicyGroup aCreateSharedPolicyGroup() {
        return CREATE_BASE.get();
    }

    private static final Supplier<UpdateSharedPolicyGroup> UPDATE_BASE = () ->
        new UpdateSharedPolicyGroup().name("My Shared Policy Group updated").description("My Shared Policy Group description updated");

    public static UpdateSharedPolicyGroup aUpdateSharedPolicyGroup() {
        return UPDATE_BASE.get();
    }

    private static final SharedPolicyGroupCRD.SharedPolicyGroupCRDBuilder CRD_BASE = SharedPolicyGroupCRD.builder()
        .crossId(UUID.random().toString())
        .name("My Shared Policy Group CRD")
        .apiType(io.gravitee.definition.model.v4.ApiType.PROXY)
        .phase(io.gravitee.apim.core.plugin.model.FlowPhase.REQUEST);

    public static SharedPolicyGroupCRD aSharedPolicyGroupCRD() {
        return CRD_BASE.build();
    }
}
