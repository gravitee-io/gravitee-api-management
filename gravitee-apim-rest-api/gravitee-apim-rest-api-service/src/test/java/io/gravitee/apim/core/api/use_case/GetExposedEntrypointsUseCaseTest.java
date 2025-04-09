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
package io.gravitee.apim.core.api.use_case;

import static fixtures.core.model.ApiFixtures.aMessageApiV4;
import static fixtures.core.model.ApiFixtures.aNativeApi;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiExposedEntrypointDomainServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ExposedEntrypoint;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GetExposedEntrypointsUseCaseTest {

    private final String API_ID = "api-id";
    private final String ENV_ID = "env-id";
    private final String ORG_ID = "org-id";
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final ApiExposedEntrypointDomainServiceInMemory apiExposedEntrypointDomainServiceInMemory =
        new ApiExposedEntrypointDomainServiceInMemory();
    private GetExposedEntrypointsUseCase getExposedEntrypointsUseCase;

    @BeforeEach
    void setUp() {
        getExposedEntrypointsUseCase = new GetExposedEntrypointsUseCase(apiCrudServiceInMemory, apiExposedEntrypointDomainServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
        apiExposedEntrypointDomainServiceInMemory.reset();
    }

    @Nested
    class WithHttpV4Api {

        private final Api API = aMessageApiV4().toBuilder().id(API_ID).environmentId(ENV_ID).build();

        @Test
        void should_return_exposed_entrypoint() {
            // Given
            apiCrudServiceInMemory.initWith(List.of(API));
            apiExposedEntrypointDomainServiceInMemory.initWith(List.of(new ExposedEntrypoint("http://myapi.domain.com")));

            // When
            var output = getExposedEntrypointsUseCase.execute(new GetExposedEntrypointsUseCase.Input(ORG_ID, ENV_ID, API_ID));

            // Then
            assertNotNull(output);
            assertEquals("http://myapi.domain.com", output.exposedEntrypoints().get(0).value());
        }
    }

    @Nested
    class WithNativeV4Api {

        private final Api API = aNativeApi().toBuilder().id(API_ID).environmentId(ENV_ID).build();

        @Test
        void should_return_exposed_entrypoint() {
            // Given
            apiCrudServiceInMemory.initWith(List.of(API));
            apiExposedEntrypointDomainServiceInMemory.initWith(List.of(new ExposedEntrypoint("myapi.kafka.domain.com:9092")));

            // When
            var output = getExposedEntrypointsUseCase.execute(new GetExposedEntrypointsUseCase.Input(ORG_ID, ENV_ID, API_ID));

            // Then
            assertNotNull(output);
            assertEquals("myapi.kafka.domain.com:9092", output.exposedEntrypoints().get(0).value());
        }
    }
}
