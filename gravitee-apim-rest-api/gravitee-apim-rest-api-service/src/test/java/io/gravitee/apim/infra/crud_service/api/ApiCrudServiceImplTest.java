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
package io.gravitee.apim.infra.crud_service.api;

import static org.mockito.Mockito.*;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.ApiCRD;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.*;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ApiCrudServiceImplTest {

    ApiRepository apiRepository;

    ApiCrudServiceImpl service;

    private static final String API_ID = "api-id";

    @BeforeEach
    void setUp() {
        apiRepository = mock(ApiRepository.class);

        service = new ApiCrudServiceImpl(apiRepository);
    }

    @Nested
    class Get {

        @Test
        void should_get_a_page() throws TechnicalException {
            var existingApi = Api
                .builder()
                .id(API_ID)
                .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                .lifecycleState(LifecycleState.STARTED)
                .build();
            when(apiRepository.findById(API_ID)).thenReturn(Optional.of(existingApi));

            var expectedApi = io.gravitee.apim.core.api.model.Api
                .builder()
                .id(API_ID)
                .apiLifecycleState(io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED)
                .lifecycleState(io.gravitee.apim.core.api.model.Api.LifecycleState.STARTED)
                .definitionContext(new ApiCRD.DefinitionContext("MANAGEMENT", "FULLY_MANAGED", "MANAGEMENT"))
                .build();

            var foundPage = service.get(API_ID);
            Assertions.assertThat(foundPage).usingRecursiveComparison().isEqualTo(expectedApi);
        }

        @Test
        void should_throw_exception_if_page_not_found() throws TechnicalException {
            when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(() -> service.get(API_ID)).isInstanceOf(ApiNotFoundException.class);
        }
    }
}
