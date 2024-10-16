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
package io.gravitee.apim.infra.scoring;

import static io.gravitee.definition.model.v4.ApiType.PROXY;
import static io.gravitee.exchange.api.command.CommandStatus.ERROR;
import static io.gravitee.rest.api.service.InstallationService.COCKPIT_INSTALLATION_ID;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static io.gravitee.rest.api.service.common.UuidString.*;
import static io.gravitee.spec.gen.api.EndpointType.OPEN_API;
import static io.gravitee.spec.gen.api.Operation.GET_STATE;
import static io.gravitee.spec.gen.api.Operation.POST_JOB;
import static io.gravitee.spec.gen.api.SpecGenRequestState.UNAVAILABLE;

import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.specgen.SpecGenCommandPayload;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestCommand;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestReply;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.spec.gen.api.Operation;
import io.gravitee.spec.gen.api.SpecGenRequest;
import io.reactivex.rxjava3.core.Single;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class SpecGenService {

    private final CockpitConnector cockpitConnector;
    private final InstallationService installationService;
    private final ApiRepository apiRepository;

    public SpecGenService(
        @Lazy CockpitConnector cockpitConnector,
        @Lazy InstallationService installationService,
        @Lazy ApiRepository apiRepository
    ) {
        this.cockpitConnector = cockpitConnector;
        this.installationService = installationService;
        this.apiRepository = apiRepository;
    }

    public Single<SpecGenRequestReply> getState(String apiId) {
        try {
            return performCommand(apiId, GET_STATE);
        } catch (Exception e) {
            log.error("An unexpected error has occurred", e);
            return Single.just(new SpecGenRequestReply(null, ERROR, UNAVAILABLE));
        }
    }

    public Single<SpecGenRequestReply> postJob(String apiId) {
        try {
            return performCommand(apiId, POST_JOB);
        } catch (Exception e) {
            log.error("An unexpected error has occurred", e);
            return Single.just(new SpecGenRequestReply(null, ERROR, UNAVAILABLE));
        }
    }

    @NotNull
    @SneakyThrows
    private Single<SpecGenRequestReply> performCommand(String apiId, Operation operation) {
        return apiRepository
            .findById(apiId)
            .filter(api -> PROXY.equals(api.getType()))
            .map(api -> new SpecGenRequestCommand(buildPayload(api.getId(), operation)))
            .map(command -> cockpitConnector.sendCommand(command).cast(SpecGenRequestReply.class))
            .orElse(Single.just(new SpecGenRequestReply(null, ERROR, UNAVAILABLE)));
    }

    @NotNull
    private SpecGenCommandPayload<SpecGenRequest> buildPayload(String apiId, Operation operation) {
        var executionContext = getExecutionContext();
        return new SpecGenCommandPayload<>(
            generateRandom(),
            executionContext.getOrganizationId(),
            executionContext.getEnvironmentId(),
            installationService.get().getAdditionalInformation().get(COCKPIT_INSTALLATION_ID),
            new SpecGenRequest(apiId, OPEN_API, operation)
        );
    }
}
