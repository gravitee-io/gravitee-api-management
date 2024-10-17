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

import static io.gravitee.cockpit.api.command.v1.CockpitCommandType.SPEC_GEN_REQUEST;
import static io.gravitee.exchange.api.command.CommandStatus.ERROR;
import static io.gravitee.exchange.api.command.CommandStatus.SUCCEEDED;
import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static io.gravitee.spec.gen.api.Operation.GET_STATE;
import static io.gravitee.spec.gen.api.Operation.POST_JOB;
import static io.gravitee.spec.gen.api.SpecGenRequestState.UNAVAILABLE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.apim.core.specgen.service_provider.SpecGenProvider;
import io.gravitee.apim.core.specgen.use_case.SpecGenRequestUseCase;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestReply;
import io.gravitee.repository.management.model.Api;
import io.gravitee.spec.gen.api.SpecGenRequestState;
import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class SpecGenRequestUseCaseTest {

    @Mock
    ApiSpecGenQueryService apiSpecGenQueryService;

    @Mock
    SpecGenProvider specGenProvider;

    private SpecGenRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SpecGenRequestUseCase(apiSpecGenQueryService, specGenProvider);
    }

    @Test
    void must_return_unavailable_get_state_with_absent_api() {
        when(apiSpecGenQueryService.findByIdAndType(any(), any(), any())).thenReturn(Optional.empty());

        useCase
            .getState(generateRandom())
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> isNull(reply.getCommandId()))
            .assertValue(reply -> SPEC_GEN_REQUEST.name().equals(reply.getType()))
            .assertValue(reply -> ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> UNAVAILABLE.equals(reply.getRequestState()));
    }

    @Test
    void must_return_unavailable_post_job_with_absent_api() {
        when(apiSpecGenQueryService.findByIdAndType(any(), any(), any())).thenReturn(Optional.empty());

        useCase
            .postJob(generateRandom())
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> isNull(reply.getCommandId()))
            .assertValue(reply -> SPEC_GEN_REQUEST.name().equals(reply.getType()))
            .assertValue(reply -> ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> UNAVAILABLE.equals(reply.getRequestState()));
    }

    public Stream<SpecGenRequestState> params_that_must_return_request_state() {
        return Arrays.stream(SpecGenRequestState.values());
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_request_state")
    void must_return_post_job_request_state(SpecGenRequestState state) {
        final String apiId = generateRandom();
        when(apiSpecGenQueryService.findByIdAndType(any(), any(), any())).thenReturn(Optional.of(Api.builder().id(apiId).build()));
        when(specGenProvider.performRequest(apiId, POST_JOB))
            .thenReturn(Single.just(new SpecGenRequestReply(generateRandom(), SUCCEEDED, state)));

        useCase
            .postJob(apiId)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> SPEC_GEN_REQUEST.name().equals(reply.getType()))
            .assertValue(reply -> SUCCEEDED.equals(reply.getCommandStatus()))
            .assertValue(reply -> state.equals(reply.getRequestState()));
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_request_state")
    void must_return_get_state_request_state(SpecGenRequestState state) {
        final String apiId = generateRandom();
        when(apiSpecGenQueryService.findByIdAndType(any(), any(), any())).thenReturn(Optional.of(Api.builder().id(apiId).build()));
        when(specGenProvider.performRequest(apiId, GET_STATE))
            .thenReturn(Single.just(new SpecGenRequestReply(generateRandom(), SUCCEEDED, state)));

        useCase
            .getState(apiId)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> SPEC_GEN_REQUEST.name().equals(reply.getType()))
            .assertValue(reply -> SUCCEEDED.equals(reply.getCommandStatus()))
            .assertValue(reply -> state.equals(reply.getRequestState()));
    }
}
