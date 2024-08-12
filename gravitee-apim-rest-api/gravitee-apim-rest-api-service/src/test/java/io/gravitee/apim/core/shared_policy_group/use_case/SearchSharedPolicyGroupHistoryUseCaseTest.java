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
import inmemory.SharedPolicyGroupHistoryQueryServiceInMemory;
import inmemory.SharedPolicyGroupQueryServiceInMemory;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchSharedPolicyGroupHistoryUseCaseTest {

    static final String ENV_ID = SharedPolicyGroupFixtures.aSharedPolicyGroup().getEnvironmentId();

    private final SharedPolicyGroupHistoryQueryServiceInMemory sharedPolicyGroupHistoryQueryService =
        new SharedPolicyGroupHistoryQueryServiceInMemory();
    private SearchSharedPolicyGroupHistoryUseCase searchSharedPolicyGroupHistoryUseCase;

    @BeforeEach
    void setUp() {
        searchSharedPolicyGroupHistoryUseCase = new SearchSharedPolicyGroupHistoryUseCase(sharedPolicyGroupHistoryQueryService);
    }

    @Test
    void should_return_shared_policy_group_histories() {
        // Given
        SharedPolicyGroup sharedPolicyGroupV1 = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupV1.setName("Fox SPG");
        SharedPolicyGroup sharedPolicyGroupV2 = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupV2.setName("Fox SPG renamed");
        SharedPolicyGroup wolfSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        wolfSharedPolicyGroup.setId("wolfSharedPolicyGroupId");
        wolfSharedPolicyGroup.setName("Wolf SPG");
        sharedPolicyGroupHistoryQueryService.initWith(List.of(sharedPolicyGroupV1, sharedPolicyGroupV2, wolfSharedPolicyGroup));

        // When
        var result = searchSharedPolicyGroupHistoryUseCase.execute(
            new SearchSharedPolicyGroupHistoryUseCase.Input(ENV_ID, "sharedPolicyGroupId", null, null)
        );
        // Then
        assertEquals(2, result.result().getTotalElements());
        assertEquals(sharedPolicyGroupV2, result.result().getContent().get(0));
        assertEquals(sharedPolicyGroupV1, result.result().getContent().get(1));
    }

    @Test
    void should_return_shared_policy_group_histories_with_pagination_and_sorting() {
        // Given
        SharedPolicyGroup sharedPolicyGroupV1 = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupV1.setName("Fox SPG");
        sharedPolicyGroupV1.setVersion(1);
        SharedPolicyGroup sharedPolicyGroupV2 = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        sharedPolicyGroupV2.setName("Fox SPG renamed");
        sharedPolicyGroupV1.setVersion(2);
        SharedPolicyGroup wolfSharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
        wolfSharedPolicyGroup.setId("wolfSharedPolicyGroupId");
        wolfSharedPolicyGroup.setName("Wolf SPG");
        sharedPolicyGroupHistoryQueryService.initWith(List.of(sharedPolicyGroupV1, sharedPolicyGroupV2, wolfSharedPolicyGroup));

        // When
        var resultASC = searchSharedPolicyGroupHistoryUseCase.execute(
            new SearchSharedPolicyGroupHistoryUseCase.Input(ENV_ID, "sharedPolicyGroupId", new PageableImpl(1, 1), "version")
        );

        var resultPage2DSC = searchSharedPolicyGroupHistoryUseCase.execute(
            new SearchSharedPolicyGroupHistoryUseCase.Input(ENV_ID, "sharedPolicyGroupId", new PageableImpl(2, 1), "-version")
        );

        // Then
        assertEquals(2, resultASC.result().getTotalElements());
        assertEquals(1, resultASC.result().getContent().size());
        assertEquals(sharedPolicyGroupV2, resultASC.result().getContent().get(0));

        assertEquals(2, resultPage2DSC.result().getTotalElements());
        assertEquals(1, resultPage2DSC.result().getContent().size());
        assertEquals(sharedPolicyGroupV2, resultPage2DSC.result().getContent().get(0));
    }

    @Test
    void should_throw_exception_when_invalid_sort_by_field() {
        // Given
        sharedPolicyGroupHistoryQueryService.initWith(List.of());

        // When
        var throwable = assertThrows(
            IllegalArgumentException.class,
            () ->
                searchSharedPolicyGroupHistoryUseCase.execute(
                    new SearchSharedPolicyGroupHistoryUseCase.Input(ENV_ID, "sharedPolicyGroupId", new PageableImpl(1, 2), "invalid")
                )
        );

        // Then
        assertEquals("Invalid sort by field: invalid", throwable.getMessage());
    }

    @Test
    void should_throw_exception_when_shared_policy_group_id_is_null() {
        // Given
        sharedPolicyGroupHistoryQueryService.initWith(List.of());

        // When
        var throwable = assertThrows(
            IllegalArgumentException.class,
            () ->
                searchSharedPolicyGroupHistoryUseCase.execute(
                    new SearchSharedPolicyGroupHistoryUseCase.Input(ENV_ID, null, new PageableImpl(1, 2), "version")
                )
        );

        // Then
        assertEquals("SharedPolicyGroupId is required", throwable.getMessage());
    }
}
