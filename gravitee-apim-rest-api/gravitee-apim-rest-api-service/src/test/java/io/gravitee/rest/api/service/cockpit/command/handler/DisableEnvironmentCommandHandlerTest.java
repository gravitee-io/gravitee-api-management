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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.environment.DisableEnvironmentCommand;
import io.gravitee.cockpit.api.command.v1.environment.DisableEnvironmentCommandPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.v4.ApiStateService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisableEnvironmentCommandHandlerTest {

    private static final String ENV_COCKPIT_ID = "env#cockpit#id";
    private static final String ENV_APIM_ID = "env#apim#id";
    private static final String API_ID = "env#api#id";
    private static final String USER_ID = "user#id";
    private static final String DICTIONARY_ID = "dictionary#id";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiStateService apiStateService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private AccessPointCrudService accessPointService;

    @Mock
    private IdentityProviderActivationService idpActivationService;

    @Mock
    private DictionaryService dictionaryService;

    private DisableEnvironmentCommandHandler cut;

    @BeforeEach
    void setUp() {
        cut =
            new DisableEnvironmentCommandHandler(
                environmentService,
                apiStateService,
                apiRepository,
                accessPointService,
                idpActivationService,
                dictionaryService
            );
    }

    @Test
    void supportType() {
        assertEquals(CockpitCommandType.DISABLE_ENVIRONMENT.name(), cut.supportType());
    }

    @Test
    void handleSuccessfulCommand() {
        var apimEnvironment = EnvironmentEntity.builder().id(ENV_APIM_ID).build();
        var context = new ExecutionContext(apimEnvironment);
        var dictionary = DictionaryEntity.builder().id(DICTIONARY_ID).build();
        when(environmentService.findByCockpitId(ENV_COCKPIT_ID)).thenReturn(apimEnvironment);
        when(
            apiRepository.search(
                eq(new ApiCriteria.Builder().environmentId(ENV_APIM_ID).state(LifecycleState.STARTED).build()),
                any(ApiFieldFilter.class)
            )
        )
            .thenReturn(List.of(Api.builder().id(API_ID).build()));
        when(dictionaryService.findAll(context)).thenReturn(Set.of(dictionary));

        cut
            .handle(aDisableEnvCommand())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValue(reply -> reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        verify(apiStateService).stop(eq(context), eq(API_ID), eq(USER_ID));
        verify(accessPointService).deleteAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, ENV_APIM_ID);
        verify(dictionaryService).stop(context, DICTIONARY_ID);
        verify(idpActivationService)
            .removeAllIdpsFromTarget(
                eq(context),
                eq(new IdentityProviderActivationService.ActivationTarget(ENV_APIM_ID, IdentityProviderActivationReferenceType.ENVIRONMENT))
            );
    }

    @Test
    void handleThrowsException() {
        when(environmentService.findByCockpitId(ENV_COCKPIT_ID)).thenThrow(new EnvironmentNotFoundException(ENV_COCKPIT_ID));

        cut
            .handle(aDisableEnvCommand())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValue(reply -> reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    private DisableEnvironmentCommand aDisableEnvCommand() {
        return new DisableEnvironmentCommand(DisableEnvironmentCommandPayload.builder().cockpitId(ENV_COCKPIT_ID).userId(USER_ID).build());
    }
}
