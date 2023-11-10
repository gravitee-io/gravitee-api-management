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

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.designer.DeployModelPayload;
import io.gravitee.cockpit.api.command.designer.DeployModelReply;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntityResult;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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

    @Override
    public Command.Type handleType() {
        return Command.Type.DEPLOY_MODEL_COMMAND;
    }

    @Override
    public Single<DeployModelReply> handle(DeployModelCommand command) {
        DeployModelPayload payload = command.getPayload();

        String apiCrossId = payload.getModelId();
        String userId = payload.getUserId();
        String swaggerDefinition = payload.getSwaggerDefinition();
        String environmentId = payload.getEnvironmentId();
        DeploymentMode mode = DeploymentMode.fromDeployModelPayload(payload);
        List<String> labels = payload.getLabels();

        try {
            final EnvironmentEntity environment = getEnvironment(environmentId);
            ExecutionContext executionContext = new ExecutionContext(environment);
            final UserEntity user = userService.findBySource(executionContext.getOrganizationId(), "cockpit", userId, true);

            authenticateAs(user);

            ApiEntityResult result;

            final Optional<String> optApiId = apiSearchService.findIdByEnvironmentIdAndCrossId(
                executionContext.getEnvironmentId(),
                apiCrossId
            );
            if (optApiId.isPresent()) {
                final String apiId = optApiId.get();
                var message = permissionChecker.checkUpdatePermission(
                    executionContext,
                    user.getId(),
                    executionContext.getEnvironmentId(),
                    apiId,
                    mode
                );

                if (message.isPresent()) {
                    var reply = new DeployModelReply(command.getId(), CommandStatus.FAILED);
                    reply.setMessage(message.get());
                    return Single.just(reply);
                }

                result =
                    cockpitApiService.updateApi(
                        executionContext,
                        apiId,
                        user.getId(),
                        swaggerDefinition,
                        executionContext.getEnvironmentId(),
                        mode,
                        labels
                    );
            } else {
                var message = permissionChecker.checkCreatePermission(
                    executionContext,
                    user.getId(),
                    executionContext.getEnvironmentId(),
                    mode
                );

                if (message.isPresent()) {
                    var reply = new DeployModelReply(command.getId(), CommandStatus.FAILED);
                    reply.setMessage(message.get());
                    return Single.just(reply);
                }

                result =
                    cockpitApiService.createApi(
                        executionContext,
                        apiCrossId,
                        user.getId(),
                        swaggerDefinition,
                        executionContext.getEnvironmentId(),
                        mode,
                        labels
                    );
            }

            if (result.isSuccess()) {
                logger.info("Api imported [{}].", result.getApi().getId());

                return Single.just(new DeployModelReply(command.getId(), CommandStatus.SUCCEEDED));
            }
            logger.error("Failed to import API [{}].", result.getErrorMessage());
            var reply = new DeployModelReply(command.getId(), CommandStatus.FAILED);
            reply.setMessage(result.getErrorMessage());
            return Single.just(reply);
        } catch (Exception e) {
            logger.error("Error occurred when importing api [{}].", payload.getModelId(), e);
            return Single.just(new DeployModelReply(command.getId(), CommandStatus.ERROR));
        }
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
