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
import static io.gravitee.definition.model.v4.ApiType.MESSAGE;
import static io.gravitee.definition.model.v4.ApiType.PROXY;
import static io.gravitee.exchange.api.command.CommandStatus.ERROR;
import static io.gravitee.exchange.api.command.CommandStatus.SUCCEEDED;
import static io.gravitee.rest.api.service.InstallationService.COCKPIT_INSTALLATION_ID;
import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static io.gravitee.spec.gen.api.SpecGenRequestState.UNAVAILABLE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestReply;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.UuidString;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SpecGenServiceTest {

    @Mock
    ApiRepository apiRepository;

    @Mock
    CockpitConnector cockpitConnector;

    @Mock
    InstallationService installationService;

    private SpecGenProviderImpl specGenService;

    @BeforeEach
    void setUp() {
        specGenService = new SpecGenProviderImpl(cockpitConnector, installationService, apiRepository);
    }

    @Test
    void must_return_unavailable_get_state_with_technical_exception() throws TechnicalException {
        when(apiRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        specGenService
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
    void must_return_unavailable_post_job_with_technical_exception() throws TechnicalException {
        when(apiRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        specGenService
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

    @Test
    void must_return_unavailable_get_state_with_absent_api() throws TechnicalException {
        when(apiRepository.findById(any())).thenReturn(Optional.empty());

        specGenService
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
    void must_return_unavailable_get_state_with_non_proxy_api() throws TechnicalException {
        var api = mock(Api.class);
        when(api.getId()).thenReturn(generateRandom());
        when(api.getType()).thenReturn(MESSAGE);

        when(apiRepository.findById(api.getId())).thenReturn(Optional.of(api));

        specGenService
            .getState(api.getId())
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
    void must_return_unavailable_post_job_with_absent_api() throws TechnicalException {
        when(apiRepository.findById(any())).thenReturn(Optional.empty());

        specGenService
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

    @Test
    void must_return_unavailable_post_job_with_non_proxy_api() throws TechnicalException {
        var api = mock(Api.class);
        when(api.getId()).thenReturn(generateRandom());
        when(api.getType()).thenReturn(MESSAGE);

        when(apiRepository.findById(api.getId())).thenReturn(Optional.of(api));

        specGenService
            .postJob(api.getId())
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
    void must_return_unavailable_get_state_with_get_state_sendCommand_error() throws TechnicalException {
        var api = mock(Api.class);
        when(api.getId()).thenReturn(generateRandom());
        when(api.getType()).thenReturn(PROXY);

        when(apiRepository.findById(api.getId())).thenReturn(Optional.of(api));

        var installationEntity = mock(InstallationEntity.class);
        var additionalInformation = Map.of(COCKPIT_INSTALLATION_ID, generateRandom());

        when(installationEntity.getAdditionalInformation()).thenReturn(additionalInformation);
        when(installationService.get()).thenReturn(installationEntity);

        when(cockpitConnector.sendCommand(any()))
            .thenReturn(Single.error(new IllegalArgumentException("An unexpected error has occurred")));

        specGenService
            .getState(api.getId())
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> SPEC_GEN_REQUEST.name().equals(reply.getType()))
            .assertValue(reply -> ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> UNAVAILABLE.equals(reply.getRequestState()));
    }

    @Test
    void must_return_unavailable_get_state_with_post_job_sendCommand_error() throws TechnicalException {
        var api = mock(Api.class);
        when(api.getId()).thenReturn(generateRandom());
        when(api.getType()).thenReturn(PROXY);

        when(apiRepository.findById(api.getId())).thenReturn(Optional.of(api));

        var installationEntity = mock(InstallationEntity.class);
        var additionalInformation = Map.of(COCKPIT_INSTALLATION_ID, generateRandom());

        when(installationEntity.getAdditionalInformation()).thenReturn(additionalInformation);
        when(installationService.get()).thenReturn(installationEntity);

        when(cockpitConnector.sendCommand(any()))
            .thenReturn(Single.error(new IllegalArgumentException("An unexpected error has occurred")));

        specGenService
            .postJob(api.getId())
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> SPEC_GEN_REQUEST.name().equals(reply.getType()))
            .assertValue(reply -> ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> UNAVAILABLE.equals(reply.getRequestState()));
    }

    public static Stream<SpecGenRequestState> params_that_must_return_state_request_reply() {
        return Arrays.stream(SpecGenRequestState.values());
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_state_request_reply")
    void must_return_state_based_on_get_state_request_reply(SpecGenRequestState state) throws TechnicalException {
        var api = mock(Api.class);
        when(api.getId()).thenReturn(generateRandom());
        when(api.getType()).thenReturn(PROXY);

        when(apiRepository.findById(api.getId())).thenReturn(Optional.of(api));

        var installationEntity = mock(InstallationEntity.class);
        var additionalInformation = Map.of(COCKPIT_INSTALLATION_ID, generateRandom());

        when(installationEntity.getAdditionalInformation()).thenReturn(additionalInformation);
        when(installationService.get()).thenReturn(installationEntity);

        when(cockpitConnector.sendCommand(any()))
            .thenReturn(Single.just(new SpecGenRequestReply(UuidString.generateRandom(), SUCCEEDED, state)));

        specGenService
            .postJob(api.getId())
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
    @MethodSource("params_that_must_return_state_request_reply")
    void must_return_state_based_on_post_job_request_reply(SpecGenRequestState state) throws TechnicalException {
        var api = mock(Api.class);
        when(api.getId()).thenReturn(generateRandom());
        when(api.getType()).thenReturn(PROXY);

        when(apiRepository.findById(api.getId())).thenReturn(Optional.of(api));

        var installationEntity = mock(InstallationEntity.class);
        var additionalInformation = Map.of(COCKPIT_INSTALLATION_ID, generateRandom());

        when(installationEntity.getAdditionalInformation()).thenReturn(additionalInformation);
        when(installationService.get()).thenReturn(installationEntity);

        when(cockpitConnector.sendCommand(any()))
            .thenReturn(Single.just(new SpecGenRequestReply(UuidString.generateRandom(), SUCCEEDED, state)));

        specGenService
            .postJob(api.getId())
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
