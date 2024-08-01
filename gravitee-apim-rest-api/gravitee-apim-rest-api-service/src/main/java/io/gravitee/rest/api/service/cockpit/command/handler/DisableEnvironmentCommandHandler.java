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

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.environment.DisableEnvironmentCommand;
import io.gravitee.cockpit.api.command.v1.environment.DisableEnvironmentReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DisableEnvironmentCommandHandler implements CommandHandler<DisableEnvironmentCommand, DisableEnvironmentReply> {

    private final EnvironmentService environmentService;
    private final ApiRepository apiRepository;
    private final ApiStateService apiStateService;
    private final AccessPointCrudService accessPointService;
    private final IdentityProviderActivationService identityProviderActivationService;
    private final DictionaryService dictionaryService;

    public DisableEnvironmentCommandHandler(
        EnvironmentService environmentService,
        ApiStateService apiStateService,
        @Lazy ApiRepository apiRepository,
        AccessPointCrudService accessPointService,
        IdentityProviderActivationService identityProviderActivationService,
        DictionaryService dictionaryService
    ) {
        this.environmentService = environmentService;
        this.apiStateService = apiStateService;
        this.apiRepository = apiRepository;
        this.accessPointService = accessPointService;
        this.identityProviderActivationService = identityProviderActivationService;
        this.dictionaryService = dictionaryService;
    }

    @Override
    public String supportType() {
        return CockpitCommandType.DISABLE_ENVIRONMENT.name();
    }

    @Override
    public Single<DisableEnvironmentReply> handle(DisableEnvironmentCommand command) {
        var payload = command.getPayload();

        try {
            var environment = environmentService.findByCockpitId(payload.cockpitId());
            var executionContext = new ExecutionContext(environment);

            // Stop all Environment APIs
            apiRepository
                .search(
                    new ApiCriteria.Builder().state(LifecycleState.STARTED).environmentId(environment.getId()).build(),
                    new ApiFieldFilter.Builder().excludeDefinition().excludePicture().build()
                )
                .forEach(api -> apiStateService.stop(executionContext, api.getId(), payload.userId()));

            // Delete related access points
            this.accessPointService.deleteAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, environment.getId());

            this.dictionaryService.findAll(executionContext)
                .forEach(dictionaryEntity -> dictionaryService.stop(executionContext, dictionaryEntity.getId()));

            // Deactivate all identity providers
            this.identityProviderActivationService.removeAllIdpsFromTarget(
                    executionContext,
                    new IdentityProviderActivationService.ActivationTarget(
                        environment.getId(),
                        IdentityProviderActivationReferenceType.ENVIRONMENT
                    )
                );

            log.info("Environment [{}] with id [{}] has been disabled.", environment.getName(), environment.getId());
            return Single.just(new DisableEnvironmentReply(command.getId()));
        } catch (Exception e) {
            String errorDetails = "Error occurred when disabling environment [%s] with id [%s].".formatted(payload.name(), payload.id());
            log.error(errorDetails, e);
            return Single.just(new DisableEnvironmentReply(command.getId(), errorDetails));
        }
    }
}
