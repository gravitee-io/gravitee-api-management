/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.designer.DeployModelPayload;
import io.gravitee.cockpit.api.command.designer.DeployModelReply;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntityResult;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.cockpit.services.ApiServiceCockpit;
import io.gravitee.rest.api.service.cockpit.services.CockpitApiPermissionChecker;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DeployModelCommandHandler implements CommandHandler<DeployModelCommand, DeployModelReply> {

    private final Logger logger = LoggerFactory.getLogger(DeployModelCommandHandler.class);

    private final ApiService apiService;
    private final ApiServiceCockpit cockpitApiService;
    private final CockpitApiPermissionChecker permissionChecker;
    private final UserService userService;
    private final EnvironmentService environmentService;

    public DeployModelCommandHandler(
        ApiService apiService,
        ApiServiceCockpit cockpitApiService,
        CockpitApiPermissionChecker permissionChecker,
        UserService userService,
        EnvironmentService environmentService
    ) {
        this.apiService = apiService;
        this.cockpitApiService = cockpitApiService;
        this.permissionChecker = permissionChecker;
        this.userService = userService;
        this.environmentService = environmentService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.DEPLOY_MODEL_COMMAND;
    }

    @Override
    public Single<DeployModelReply> handle(DeployModelCommand command) {
        DeployModelPayload payload = command.getPayload();

        String apiId = payload.getModelId();
        String userId = payload.getUserId();
        String swaggerDefinition = payload.getSwaggerDefinition();
        String environmentId = payload.getEnvironmentId();
        DeploymentMode mode = DeploymentMode.fromDeployModelPayload(payload);

        try {
            final EnvironmentEntity environment = environmentService.findByCockpitId(environmentId);
            GraviteeContext.setCurrentEnvironment(environment.getId());
            GraviteeContext.setCurrentOrganization(environment.getOrganizationId());
            final UserEntity user = userService.findBySource("cockpit", userId, false);

            ApiEntityResult result;

            if (apiService.exists(apiId)) {
                var message = permissionChecker.checkUpdatePermission(user.getId(), environment.getId(), apiId, mode);

                if (message.isPresent()) {
                    var reply = new DeployModelReply(command.getId(), CommandStatus.FAILED);
                    reply.setMessage(message.get());
                    return Single.just(reply);
                }

                result = cockpitApiService.updateApi(apiId, user.getId(), swaggerDefinition, environment.getId(), mode);
            } else {
                var message = permissionChecker.checkCreatePermission(user.getId(), environment.getId(), mode);

                if (message.isPresent()) {
                    var reply = new DeployModelReply(command.getId(), CommandStatus.FAILED);
                    reply.setMessage(message.get());
                    return Single.just(reply);
                }

                result = cockpitApiService.createApi(apiId, user.getId(), swaggerDefinition, environment.getId(), mode);
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
        } finally {
            GraviteeContext.cleanContext();
        }
    }
}
