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
package io.gravitee.apim.core.shared_policy_group.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fixtures.core.model.SharedPolicyGroupFixtures;
import inmemory.SharedPolicyGroupHistoryQueryServiceInMemory;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetSharedPolicyGroupPolicyPluginsUseCaseTest {

    static final String ENV_ID = SharedPolicyGroupFixtures.aSharedPolicyGroup().getEnvironmentId();

    private final SharedPolicyGroupHistoryQueryServiceInMemory sharedPolicyGroupHistoryQueryServiceInMemory =
        new SharedPolicyGroupHistoryQueryServiceInMemory();
    private GetSharedPolicyGroupPolicyPluginsUseCase getSharedPolicyGroupPolicyPluginsUseCase;

    @BeforeEach
    void setUp() {
        getSharedPolicyGroupPolicyPluginsUseCase =
            new GetSharedPolicyGroupPolicyPluginsUseCase(sharedPolicyGroupHistoryQueryServiceInMemory);
    }

    @Test
    void should_return_shared_policy_group() {
        // Given
        SharedPolicyGroup deployedSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        deployedSharedPolicyGroup.setLifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED);

        SharedPolicyGroup undeployedSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        undeployedSharedPolicyGroup.setLifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED);
        sharedPolicyGroupHistoryQueryServiceInMemory.initWith(List.of(deployedSharedPolicyGroup, undeployedSharedPolicyGroup));

        // When
        var result = getSharedPolicyGroupPolicyPluginsUseCase.execute(new GetSharedPolicyGroupPolicyPluginsUseCase.Input(ENV_ID));

        // Then
        assertEquals(result.sharedPolicyGroupPolicyList().size(), 1);
        assertEquals(result.sharedPolicyGroupPolicyList().get(0).getId(), deployedSharedPolicyGroup.getCrossId());
        assertEquals(result.sharedPolicyGroupPolicyList().get(0).getName(), deployedSharedPolicyGroup.getName());
        assertEquals(result.sharedPolicyGroupPolicyList().get(0).getDescription(), deployedSharedPolicyGroup.getDescription());
        assertEquals(
            result.sharedPolicyGroupPolicyList().get(0).getPrerequisiteMessage(),
            deployedSharedPolicyGroup.getPrerequisiteMessage()
        );
        assertEquals(result.sharedPolicyGroupPolicyList().get(0).getApiType(), deployedSharedPolicyGroup.getApiType());
        assertEquals(result.sharedPolicyGroupPolicyList().get(0).getPhase(), deployedSharedPolicyGroup.getPhase());
        assertEquals(result.sharedPolicyGroupPolicyList().get(0).getPolicyId(), "shared-policy-group-policy");
    }
}
