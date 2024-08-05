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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fixtures.core.model.SharedPolicyGroupFixtures;
import inmemory.SharedPolicyGroupQueryServiceInMemory;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchSharedPolicyGroupUseCaseTest {

    static final String ENV_ID = SharedPolicyGroupFixtures.aSharedPolicyGroup().getEnvironmentId();

    private final SharedPolicyGroupQueryServiceInMemory sharedPolicyGroupQueryService = new SharedPolicyGroupQueryServiceInMemory();
    private SearchSharedPolicyGroupUseCase searchSharedPolicyGroupUseCase;

    @BeforeEach
    void setUp() {
        searchSharedPolicyGroupUseCase = new SearchSharedPolicyGroupUseCase(sharedPolicyGroupQueryService);
    }

    @Test
    void should_return_shared_policy_group() {
        // Given
        SharedPolicyGroup expectedSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupQueryService.initWith(List.of(expectedSharedPolicyGroup));

        // When
        var result = searchSharedPolicyGroupUseCase.execute(new SearchSharedPolicyGroupUseCase.Input(ENV_ID, null, null, null));
        // Then
        assertEquals(1, result.result().getTotalElements());
        assertEquals(expectedSharedPolicyGroup, result.result().getContent().get(0));
    }

    @Test
    void should_return_shared_policy_group_with_name() {
        // Given
        SharedPolicyGroup expectedSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        expectedSharedPolicyGroup.setName("Fox SPG");
        SharedPolicyGroup anotherSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        anotherSharedPolicyGroup.setName("Wolf SPG");
        sharedPolicyGroupQueryService.initWith(List.of(expectedSharedPolicyGroup, anotherSharedPolicyGroup));

        // When
        var result = searchSharedPolicyGroupUseCase.execute(new SearchSharedPolicyGroupUseCase.Input(ENV_ID, "Fox", null, null));
        // Then
        assertEquals(1, result.result().getTotalElements());
        assertEquals(expectedSharedPolicyGroup, result.result().getContent().get(0));
    }

    @Test
    void should_return_shared_policy_group_with_pagination_and_sorting() {
        // Given
        SharedPolicyGroup foxSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        foxSharedPolicyGroup.setName("Fox SPG");
        SharedPolicyGroup wolfSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        wolfSharedPolicyGroup.setName("Wolf SPG");
        SharedPolicyGroup beartSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        beartSharedPolicyGroup.setName("Bear SPG");
        sharedPolicyGroupQueryService.initWith(List.of(foxSharedPolicyGroup, wolfSharedPolicyGroup, beartSharedPolicyGroup));

        // When
        var resultASC = searchSharedPolicyGroupUseCase.execute(
            new SearchSharedPolicyGroupUseCase.Input(ENV_ID, null, new PageableImpl(1, 2), "name")
        );

        var resultDSC = searchSharedPolicyGroupUseCase.execute(
            new SearchSharedPolicyGroupUseCase.Input(ENV_ID, null, new PageableImpl(1, 2), "-name")
        );

        // Then
        assertEquals(3, resultASC.result().getTotalElements());
        assertEquals(2, resultASC.result().getContent().size());
        assertEquals(beartSharedPolicyGroup, resultASC.result().getContent().get(0));
        assertEquals(foxSharedPolicyGroup, resultASC.result().getContent().get(1));

        assertEquals(3, resultDSC.result().getTotalElements());
        assertEquals(2, resultDSC.result().getContent().size());
        assertEquals(wolfSharedPolicyGroup, resultDSC.result().getContent().get(0));
        assertEquals(foxSharedPolicyGroup, resultDSC.result().getContent().get(1));
    }

    @Test
    void should_throw_exception_when_invalid_sort_by_field() {
        // Given
        SharedPolicyGroup foxSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        foxSharedPolicyGroup.setName("Fox SPG");
        SharedPolicyGroup wolfSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        wolfSharedPolicyGroup.setName("Wolf SPG");
        sharedPolicyGroupQueryService.initWith(List.of(foxSharedPolicyGroup, wolfSharedPolicyGroup));

        // When
        var throwable = assertThrows(
            IllegalArgumentException.class,
            () ->
                searchSharedPolicyGroupUseCase.execute(
                    new SearchSharedPolicyGroupUseCase.Input(ENV_ID, null, new PageableImpl(1, 2), "invalid")
                )
        );

        // Then
        assertEquals("Invalid sort by field: invalid", throwable.getMessage());
    }
}
