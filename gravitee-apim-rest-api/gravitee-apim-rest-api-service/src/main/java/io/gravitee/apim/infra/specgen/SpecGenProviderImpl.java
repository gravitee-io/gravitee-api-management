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

import static io.gravitee.apim.core.specgen.model.ApiSpecGenRequestState.UNAVAILABLE;
import static io.gravitee.rest.api.service.InstallationService.COCKPIT_INSTALLATION_ID;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static io.gravitee.rest.api.service.common.UuidString.*;
import static io.gravitee.spec.gen.api.EndpointType.OPEN_API;

import io.gravitee.apim.core.specgen.model.ApiSpecGenOperation;
import io.gravitee.apim.core.specgen.model.ApiSpecGenRequestReply;
import io.gravitee.apim.core.specgen.model.ApiSpecGenRequestState;
import io.gravitee.apim.core.specgen.service_provider.SpecGenProvider;
import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.specgen.SpecGenCommandPayload;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestCommand;
import io.gravitee.cockpit.api.command.v1.specgen.request.SpecGenRequestReply;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.spec.gen.api.Operation;
import io.gravitee.spec.gen.api.SpecGenRequest;
import io.reactivex.rxjava3.core.Single;
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

    public SpecGenProviderImpl(@Lazy CockpitConnector cockpitConnector, @Lazy InstallationService installationService) {
        this.cockpitConnector = cockpitConnector;
        this.installationService = installationService;
    }

    public Single<ApiSpecGenRequestReply> performRequest(String apiId, ApiSpecGenOperation operation, String userId) {
        var command = new SpecGenRequestCommand(buildPayload(apiId, Operation.valueOf(operation.name()), userId));
        return cockpitConnector
            .sendCommand(command)
            .cast(SpecGenRequestReply.class)
            .map(reply -> new ApiSpecGenRequestReply(ApiSpecGenRequestState.valueOf(reply.getRequestState().name())))
            .doOnError(t ->
                log.error("An error had occurred while sending command [{}] with payload [{}]", command.getId(), command.getPayload(), t)
            )
            .onErrorResumeWith(Single.just(new ApiSpecGenRequestReply(UNAVAILABLE)));
    }

    @NotNull
    private SpecGenCommandPayload<SpecGenRequest> buildPayload(String apiId, Operation operation, String userId) {
        var context = getExecutionContext();

        return new SpecGenCommandPayload<>(
            generateRandom(),
            context.getOrganizationId(),
            context.getEnvironmentId(),
            installationService.get().getAdditionalInformation().get(COCKPIT_INSTALLATION_ID),
            new SpecGenRequest(apiId, OPEN_API, operation, userId)
        );
    }
}
