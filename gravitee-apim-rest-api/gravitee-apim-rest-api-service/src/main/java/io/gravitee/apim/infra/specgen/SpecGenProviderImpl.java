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

import static io.gravitee.definition.model.v4.ApiType.PROXY;
import static io.gravitee.exchange.api.command.CommandStatus.ERROR;
import static io.gravitee.rest.api.service.InstallationService.COCKPIT_INSTALLATION_ID;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static io.gravitee.rest.api.service.common.UuidString.*;
import static io.gravitee.spec.gen.api.EndpointType.OPEN_API;
import static io.gravitee.spec.gen.api.Operation.GET_STATE;
import static io.gravitee.spec.gen.api.Operation.POST_JOB;
import static io.gravitee.spec.gen.api.SpecGenRequestState.UNAVAILABLE;

import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.apim.core.specgen.service_provider.SpecGenProvider;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.specgen.SpecGenCommandPayload;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestCommand;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestReply;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
public class SpecGenProviderImpl implements SpecGenProvider {

    private final CockpitConnector cockpitConnector;
    private final InstallationService installationService;
    private final ApiSpecGenQueryService apiSpecGenQueryService;

    public SpecGenProviderImpl(
        @Lazy CockpitConnector cockpitConnector,
        @Lazy InstallationService installationService,
        @Lazy ApiSpecGenQueryService apiSpecGenQueryService
    ) {
        this.cockpitConnector = cockpitConnector;
        this.installationService = installationService;
        this.apiSpecGenQueryService = apiSpecGenQueryService;
    }

    public Single<SpecGenRequestReply> getState(String apiId) {
        return performCommand(apiId, GET_STATE);
    }

    public Single<SpecGenRequestReply> postJob(String apiId) {
        return performCommand(apiId, POST_JOB);
    }

    @NotNull
    private Single<SpecGenRequestReply> performCommand(String apiId, Operation operation) {
        var context = getExecutionContext();

        return apiSpecGenQueryService
            .findByIdAndType(context, apiId, PROXY)
            .map(api -> new SpecGenRequestCommand(buildPayload(context, api.getId(), operation)))
            .map(this::sendCommand)
            .orElse(Single.just(new SpecGenRequestReply(null, ERROR, UNAVAILABLE)));
    }

    @NotNull
    private Single<SpecGenRequestReply> sendCommand(SpecGenRequestCommand command) {
        return cockpitConnector
            .sendCommand(command)
            .doOnError(t ->
                log.error("An error had occurred while sending command [{}] with payload [{}]", command.getId(), command.getPayload(), t)
            )
            .onErrorResumeWith(Single.just(new SpecGenRequestReply(command.getId(), ERROR, UNAVAILABLE)))
            .cast(SpecGenRequestReply.class);
    }

    @NotNull
    private SpecGenCommandPayload<SpecGenRequest> buildPayload(ExecutionContext context, String apiId, Operation operation) {
        return new SpecGenCommandPayload<>(
            generateRandom(),
            context.getOrganizationId(),
            context.getEnvironmentId(),
            installationService.get().getAdditionalInformation().get(COCKPIT_INSTALLATION_ID),
            new SpecGenRequest(apiId, OPEN_API, operation)
        );
    }
}
