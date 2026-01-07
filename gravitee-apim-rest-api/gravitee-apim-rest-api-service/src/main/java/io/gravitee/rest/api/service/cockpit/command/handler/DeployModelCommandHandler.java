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

import static io.gravitee.rest.api.service.common.SecurityContextHelper.authenticateAs;

import io.gravitee.apim.core.api.domain_service.cockpit.DeployModelToApiDomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.use_case.cockpit.DeployModelToApiCreateUseCase;
import io.gravitee.apim.core.api.use_case.cockpit.DeployModelToApiUpdateUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelReply;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.cockpit.services.ApiServiceCockpit;
import io.gravitee.rest.api.service.cockpit.services.CockpitApiPermissionChecker;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@CustomLog
public class DeployModelCommandHandler implements CommandHandler<DeployModelCommand, DeployModelReply> {

    private final ApiSearchService apiSearchService;
    private final ApiServiceCockpit cockpitApiService;
    private final CockpitApiPermissionChecker permissionChecker;
    private final UserService userService;
    private final EnvironmentService environmentService;
    private final DeployModelToApiCreateUseCase deployModelToApiCreateUseCase;
    private final DeployModelToApiUpdateUseCase deployModelToApiUpdateUseCase;

    @Override
    public String supportType() {
        return CockpitCommandType.DEPLOY_MODEL.name();
    }

    @Override
    public Single<DeployModelReply> handle(DeployModelCommand command) {
        var payload = command.getPayload();

        final var apiCrossId = payload.modelId();
        final var userId = payload.userId();
        final var swaggerDefinition = payload.swaggerDefinition();
        final var environmentId = payload.environmentId();
        final var mode = DeploymentMode.fromDeployModelPayload(payload);
        final var labels = payload.labels();
        final var executionContext = new ExecutionContext(getEnvironment(environmentId));
        final var user = userService.findBySource(executionContext.getOrganizationId(), "cockpit", userId, true);
        final var optApiId = apiSearchService.findIdByEnvironmentIdAndCrossId(executionContext.getEnvironmentId(), apiCrossId);

        try {
            authenticateAs(user);

            var audit = AuditInfo.builder()
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.getEnvironmentId())
                .actor(AuditActor.builder().userId(user.getId()).userSource(user.getSource()).userSourceId(user.getSourceId()).build())
                .build();

            if (optApiId.isPresent()) {
                var apiId = optApiId.get();
                var message = permissionChecker.checkUpdatePermission(
                    executionContext,
                    user.getId(),
                    executionContext.getEnvironmentId(),
                    apiId,
                    mode
                );

                return message
                    .map(s -> Single.just(new DeployModelReply(command.getId(), s)))
                    .orElseGet(() -> {
                        final Api api = apiSearchService.findRepositoryApiById(executionContext, apiId);

                        if (api.getDefinitionVersion() == DefinitionVersion.V2) {
                            return updateV2Api(command, apiId, executionContext, user, mode, swaggerDefinition, labels);
                        }

                        deployModelToApiUpdateUseCase.execute(
                            new DeployModelToApiUpdateUseCase.Input(
                                swaggerDefinition,
                                audit,
                                apiId,
                                apiCrossId,
                                fromDeploymentModel(mode),
                                labels
                            )
                        );
                        return Single.just(new DeployModelReply(command.getId()));
                    });
            } else {
                var message = permissionChecker.checkCreatePermission(
                    executionContext,
                    user.getId(),
                    executionContext.getEnvironmentId(),
                    mode
                );

                return message
                    .map(error -> Single.just(new DeployModelReply(command.getId(), error)))
                    .orElseGet(() -> {
                        deployModelToApiCreateUseCase.execute(
                            new DeployModelToApiCreateUseCase.Input(swaggerDefinition, audit, apiCrossId, fromDeploymentModel(mode), labels)
                        );
                        return Single.just(new DeployModelReply(command.getId()));
                    });
            }
        } catch (InvalidPathsException ipe) {
            log.error("Context path already used, API [crossId: {}] can be imported", apiCrossId, ipe);
            return Single.just(new DeployModelReply(command.getId(), "Failed to import API [context path not available]."));
        } catch (Exception e) {
            log.error("Error occurred when importing API [crossId: {}]", apiCrossId, e);
            return Single.just(new DeployModelReply(command.getId(), "Error occurred when importing API"));
        }
    }

    private DeployModelToApiDomainService.Mode fromDeploymentModel(DeploymentMode mode) {
        return switch (mode) {
            case API_DOCUMENTED -> DeployModelToApiDomainService.Mode.DOCUMENTED;
            case API_MOCKED -> DeployModelToApiDomainService.Mode.MOCKED;
            case API_PUBLISHED -> DeployModelToApiDomainService.Mode.PUBLISHED;
        };
    }

    private @NotNull Single<DeployModelReply> updateV2Api(
        DeployModelCommand command,
        String apiId,
        ExecutionContext executionContext,
        UserEntity user,
        DeploymentMode mode,
        String swaggerDefinition,
        List<String> labels
    ) {
        var result = cockpitApiService.updateApi(
            executionContext,
            apiId,
            user.getId(),
            swaggerDefinition,
            executionContext.getEnvironmentId(),
            mode,
            labels
        );

        log.info("API [id: {} / crossId: {}] v2 updated.", result.getApi().getId(), result.getApi().getCrossId());
        return Single.just(new DeployModelReply(command.getId()));
    }

    /**
     * To prepare future refactoring on cockpit which will always send apim reference, we first search by cockpit id then by apim id.
     *
     * @param environmentId the env id which could be a cockpit id or apim environment id
     * @return {@link EnvironmentEntity} found
     */
    private EnvironmentEntity getEnvironment(final String environmentId) {
        try {
            return environmentService.findByCockpitId(environmentId);
        } catch (EnvironmentNotFoundException e) {
            return environmentService.findById(environmentId);
        }
    }
}
