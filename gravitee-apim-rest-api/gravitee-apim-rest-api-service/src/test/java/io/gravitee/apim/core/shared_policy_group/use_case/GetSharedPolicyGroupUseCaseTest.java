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

import static org.junit.jupiter.api.Assertions.*;

import fixtures.core.model.SharedPolicyGroupFixtures;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupNotFoundException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetSharedPolicyGroupUseCaseTest {

    static final String ENV_ID = SharedPolicyGroupFixtures.aSharedPolicyGroup().getEnvironmentId();

    private final SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService = new SharedPolicyGroupCrudServiceInMemory();
    private GetSharedPolicyGroupUseCase getSharedPolicyGroupUseCase;

    @BeforeEach
    void setUp() {
        getSharedPolicyGroupUseCase = new GetSharedPolicyGroupUseCase(sharedPolicyGroupCrudService);
    }

    @Test
    void should_return_shared_policy_group() {
        // Given
        SharedPolicyGroup expectedSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(expectedSharedPolicyGroup));
        // When
        var result = getSharedPolicyGroupUseCase.execute(new GetSharedPolicyGroupUseCase.Input(ENV_ID, expectedSharedPolicyGroup.getId()));
        // Then
        assertEquals(expectedSharedPolicyGroup, result.sharedPolicyGroup());
    }

    @Test
    void should_throw_exception_when_shared_policy_group_not_found() {
        // Given
        SharedPolicyGroup expectedSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(expectedSharedPolicyGroup));
        // When
        var throwable = assertThrows(
            SharedPolicyGroupNotFoundException.class,
            () -> getSharedPolicyGroupUseCase.execute(new GetSharedPolicyGroupUseCase.Input(ENV_ID, "unknown"))
        );
        // Then
        assertEquals("SharedPolicyGroup [unknown] cannot be found.", throwable.getMessage());
    }

    @Test
    void should_throw_exception_when_shared_policy_group_not_found_in_environment() {
        // Given
        SharedPolicyGroup expectedSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupCrudService.initWith(List.of(expectedSharedPolicyGroup));
        // When
        var throwable = assertThrows(
            SharedPolicyGroupNotFoundException.class,
            () -> getSharedPolicyGroupUseCase.execute(new GetSharedPolicyGroupUseCase.Input("unknown", expectedSharedPolicyGroup.getId()))
        );
        // Then
        assertEquals("SharedPolicyGroup [" + expectedSharedPolicyGroup.getId() + "] cannot be found.", throwable.getMessage());
    }
}
