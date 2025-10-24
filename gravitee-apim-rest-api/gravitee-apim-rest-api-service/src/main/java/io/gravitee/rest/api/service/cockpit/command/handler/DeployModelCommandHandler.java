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

import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.use_case.OAIToImportApiUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelReply;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
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
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
public class DeployModelCommandHandler implements CommandHandler<DeployModelCommand, DeployModelReply> {

    private final Logger logger = LoggerFactory.getLogger(DeployModelCommandHandler.class);

    private final ApiSearchService apiSearchService;
    private final ApiServiceCockpit cockpitApiService;
    private final CockpitApiPermissionChecker permissionChecker;
    private final UserService userService;
    private final EnvironmentService environmentService;
    private final OAIToImportApiUseCase oaiToImportApiUseCase;

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
                var message = permissionChecker.checkUpdatePermission(
                    executionContext,
                    user.getId(),
                    executionContext.getEnvironmentId(),
                    optApiId.get(),
                    mode
                );

                return message
                    .map(s -> Single.just(new DeployModelReply(command.getId(), s)))
                    .orElseGet(() -> {
                        final ApiEntity api = apiSearchService.findById(executionContext, optApiId.get());

                        if (api.getDefinitionVersion() == DefinitionVersion.V2) {
                            return updateV2Api(command, optApiId.get(), executionContext, user, mode, swaggerDefinition, labels);
                        }

                        logger.warn("Update of v4 API is not yet supported.");
                        return Single.just(new DeployModelReply(command.getId(), "Update of v4 API is not yet supported."));
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
                    .orElseGet(() ->
                        deployV4Api(command, executionContext, configure(mode, swaggerDefinition), audit, mode, apiCrossId, labels)
                    );
            }
        } catch (InvalidPathsException ipe) {
            final var errorDetails = "Failed to import API [context path not available].";
            logger.error(errorDetails, ipe);
            return Single.just(new DeployModelReply(command.getId(), errorDetails));
        } catch (Exception e) {
            final var errorDetails = "Error occurred when importing api [%s]".formatted(payload.modelId());
            logger.error(errorDetails, e);
            return Single.just(new DeployModelReply(command.getId(), errorDetails));
        }
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

        logger.info("Api v2 updated [{}].", result.getApi().getId());
        return Single.just(new DeployModelReply(command.getId()));
    }

    private @NotNull Single<DeployModelReply> deployV4Api(
        DeployModelCommand command,
        ExecutionContext executionContext,
        ImportSwaggerDescriptorEntity importSwaggerDescriptor,
        AuditInfo audit,
        DeploymentMode mode,
        String apiCrossId,
        List<String> labels
    ) {
        var importOutput = oaiToImportApiUseCase.execute(new OAIToImportApiUseCase.Input(importSwaggerDescriptor, true, false, audit));
        cockpitApiService.manageV4Api(executionContext, audit, mode, importOutput.apiWithFlows(), apiCrossId, labels);

        logger.info("Api v4 published from Cockpit [{}].", importOutput.apiWithFlows().getId());
        return Single.just(new DeployModelReply(command.getId()));
    }

    private ImportSwaggerDescriptorEntity configure(DeploymentMode mode, String swaggerDefinition) {
        var importSwaggerDescriptor = ImportSwaggerDescriptorEntity.builder().payload(swaggerDefinition);

        logger.debug("API will be Documented.");
        importSwaggerDescriptor.withDocumentation(true);

        if (mode == DeploymentMode.API_MOCKED || mode == DeploymentMode.API_PUBLISHED) {
            logger.debug("API will be Mocked.");
            importSwaggerDescriptor.withPolicyPaths(true);
            importSwaggerDescriptor.withPolicies(List.of("mock"));
        }

        return importSwaggerDescriptor.build();
    }

    /**
     * In order to prepare future refactoring on cockpit which will always send apim reference, we first search by cockpit id then by apim id.
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
