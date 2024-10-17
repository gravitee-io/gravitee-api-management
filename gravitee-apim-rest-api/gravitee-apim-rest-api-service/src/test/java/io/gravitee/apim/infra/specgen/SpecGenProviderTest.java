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
package io.gravitee.apim.infra.specgen;

import static io.gravitee.cockpit.api.command.v1.CockpitCommandType.SPEC_GEN_REQUEST;
import static io.gravitee.definition.model.v4.ApiType.PROXY;
import static io.gravitee.exchange.api.command.CommandStatus.ERROR;
import static io.gravitee.exchange.api.command.CommandStatus.SUCCEEDED;
import static io.gravitee.rest.api.service.InstallationService.COCKPIT_INSTALLATION_ID;
import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static io.gravitee.spec.gen.api.Operation.GET_STATE;
import static io.gravitee.spec.gen.api.Operation.POST_JOB;
import static io.gravitee.spec.gen.api.SpecGenRequestState.AVAILABLE;
import static io.gravitee.spec.gen.api.SpecGenRequestState.GENERATING;
import static io.gravitee.spec.gen.api.SpecGenRequestState.STARTED;
import static io.gravitee.spec.gen.api.SpecGenRequestState.UNAVAILABLE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestReply;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.spec.gen.api.Operation;
import io.gravitee.spec.gen.api.SpecGenRequestState;
import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SpecGenProviderTest {

    @Mock
    CockpitConnector cockpitConnector;

    @Mock
    InstallationService installationService;

    private SpecGenProviderImpl specGenProvider;

    @BeforeEach
    void setUp() {
        specGenProvider = new SpecGenProviderImpl(cockpitConnector, installationService);
    }

    public static Stream<Operation> params_that_must_return_state_request_reply_due_to_error() {
        return Arrays.stream(Operation.values());
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_state_request_reply_due_to_error")
    void must_return_state_request_reply_due_to_error(Operation operation) {
        var installationEntity = mock(InstallationEntity.class);
        var additionalInformation = Map.of(COCKPIT_INSTALLATION_ID, generateRandom());

        when(installationEntity.getAdditionalInformation()).thenReturn(additionalInformation);
        when(installationService.get()).thenReturn(installationEntity);

        when(cockpitConnector.sendCommand(any()))
            .thenReturn(Single.error(new IllegalArgumentException("An unexpected error has occurred")));

        specGenProvider
            .performRequest(generateRandom(), operation)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> SPEC_GEN_REQUEST.name().equals(reply.getType()))
            .assertValue(reply -> ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> UNAVAILABLE.equals(reply.getRequestState()));
    }

    public static Stream<Arguments> params_that_must_return_state_request_reply() {
        return Stream.of(
            Arguments.of(AVAILABLE, GET_STATE),
            Arguments.of(UNAVAILABLE, GET_STATE),
            Arguments.of(STARTED, GET_STATE),
            Arguments.of(GENERATING, GET_STATE),
            Arguments.of(AVAILABLE, POST_JOB),
            Arguments.of(UNAVAILABLE, POST_JOB),
            Arguments.of(STARTED, POST_JOB),
            Arguments.of(GENERATING, POST_JOB)
        );
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_state_request_reply")
    void must_return_state_request_reply(SpecGenRequestState state, Operation operation) {
        var installationEntity = mock(InstallationEntity.class);
        var additionalInformation = Map.of(COCKPIT_INSTALLATION_ID, generateRandom());

        when(installationEntity.getAdditionalInformation()).thenReturn(additionalInformation);
        when(installationService.get()).thenReturn(installationEntity);

        when(cockpitConnector.sendCommand(any()))
            .thenReturn(Single.just(new SpecGenRequestReply(UuidString.generateRandom(), SUCCEEDED, state)));

        specGenProvider
            .performRequest(generateRandom(), operation)
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
