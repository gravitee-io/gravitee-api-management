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
package io.gravitee.apim.core.specgen.usecase;

import static io.gravitee.rest.api.service.common.UuidString.generateRandom;

import inmemory.ApiSpecGenQueryServiceInMemory;
import inmemory.OasProviderInMemory;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.use_case.BuildSpecGenPageResponseUseCase;
import io.gravitee.apim.core.specgen.use_case.BuildSpecGenPageResponseUseCase.Input;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BuildSpecGenPageResponseUseCaseTest {

    private static final String API_ID = generateRandom();
    private static final String CURRENT_ENV = "envId";
    public static final String OAS_SPEC = "openapi: 3.0.3\ninfo:\n  name: api-4\n  version: some description\n  description: some version";
    BuildSpecGenPageResponseUseCase useCase;

    static final ApiSpecGenQueryServiceInMemory queryService = new ApiSpecGenQueryServiceInMemory();
    static final OasProviderInMemory oasProvider = new OasProviderInMemory();

    @BeforeEach
    void setUp() {
        useCase = new BuildSpecGenPageResponseUseCase(queryService, oasProvider);

        GraviteeContext.setCurrentEnvironment(CURRENT_ENV);

        oasProvider.initWith(List.of(OAS_SPEC));
        queryService.initWith(
            List.of(
                new ApiSpecGen(generateRandom(), "api-1", "some description", "some-version", ApiType.MESSAGE, CURRENT_ENV),
                new ApiSpecGen(generateRandom(), "api-2", "some description", "some-version", ApiType.NATIVE, CURRENT_ENV),
                new ApiSpecGen(API_ID, "api-3", "some description", "some-version", ApiType.PROXY, "otherEnvId"),
                new ApiSpecGen(API_ID, "api-4", "some description", "some-version", ApiType.PROXY, CURRENT_ENV)
            )
        );
    }

    @Test
    void must_not_build_page_no_api_found() {
        useCase
            .execute(new Input(generateRandom(), "openapi: 3.0.3"))
            .test()
            .awaitDone(2, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertNoValues();
    }

    @Test
    void must_build_page() {
        useCase
            .execute(new Input(API_ID, "openapi: 3.0.3"))
            .test()
            .awaitDone(2, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(Objects::nonNull)
            .assertValue(page -> page.getName().equals("api-4"))
            .assertValue(page -> page.getReferenceId().equals(API_ID));
    }
}
