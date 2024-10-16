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

import static io.gravitee.apim.core.specgen.model.ApiSpecGenOperation.GET_STATE;
import static io.gravitee.apim.core.specgen.model.ApiSpecGenOperation.POST_JOB;
import static io.gravitee.apim.core.specgen.model.ApiSpecGenRequestState.UNAVAILABLE;
import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import inmemory.ApiSpecGenQueryServiceInMemory;
import inmemory.SpecGenProviderInMemory;
import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.model.ApiSpecGenRequestReply;
import io.gravitee.apim.core.specgen.model.ApiSpecGenRequestState;
import io.gravitee.apim.core.specgen.service_provider.SpecGenProvider;
import io.gravitee.apim.core.specgen.use_case.SpecGenRequestUseCase;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class SpecGenRequestUseCaseTest {

    final ApiSpecGenQueryServiceInMemory queryService = new ApiSpecGenQueryServiceInMemory();
    final SpecGenProviderInMemory specGenProvider = new SpecGenProviderInMemory();

    private SpecGenRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SpecGenRequestUseCase(queryService, specGenProvider);
    }

    @Test
    void must_return_unavailable_get_state_with_absent_api() {
        final String expectedId = generateRandom();
        queryService.initWith(List.of());

        useCase
            .getState(expectedId)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(state -> UNAVAILABLE.equals(state.state()));
    }

    @Test
    void must_return_unavailable_post_job_with_absent_api() {
        final String apiId = generateRandom();
        queryService.initWith(List.of());
        useCase
            .postJob(apiId)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(state -> UNAVAILABLE.equals(state.state()));
    }

    public static Stream<ApiSpecGenRequestState> params_that_must_return_request_state() {
        return Arrays.stream(ApiSpecGenRequestState.values());
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_request_state")
    void must_return_post_job_request_state(ApiSpecGenRequestState state) {
        final String apiId = generateRandom();
        final String currentEnv = "envId";
        queryService.initWith(
            List.of(
                new ApiSpecGen(generateRandom(), ApiType.MESSAGE, currentEnv),
                new ApiSpecGen(generateRandom(), ApiType.NATIVE, currentEnv),
                new ApiSpecGen(apiId, ApiType.PROXY, "otherEnvId"),
                new ApiSpecGen(apiId, ApiType.PROXY, currentEnv)
            )
        );
        GraviteeContext.setCurrentEnvironment(currentEnv);

        specGenProvider.initWith(List.of(Single.just(new ApiSpecGenRequestReply(state))));

        useCase
            .postJob(apiId)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> state.equals(reply.state()));
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_request_state")
    void must_return_get_state_request_state(ApiSpecGenRequestState state) {
        final String apiId = generateRandom();
        final String currentEnv = "envId";
        queryService.initWith(
            List.of(
                new ApiSpecGen(generateRandom(), ApiType.MESSAGE, currentEnv),
                new ApiSpecGen(generateRandom(), ApiType.NATIVE, currentEnv),
                new ApiSpecGen(generateRandom(), ApiType.PROXY, "otherEnvId"),
                new ApiSpecGen(apiId, ApiType.PROXY, currentEnv)
            )
        );

        specGenProvider.initWith(List.of(Single.just(new ApiSpecGenRequestReply(state))));

        useCase
            .getState(apiId)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> state.equals(reply.state()));
    }
}
