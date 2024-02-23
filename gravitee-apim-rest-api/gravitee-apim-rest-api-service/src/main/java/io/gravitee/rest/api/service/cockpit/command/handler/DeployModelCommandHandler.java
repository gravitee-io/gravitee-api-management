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

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelCommandPayload;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelReply;
import io.gravitee.exchange.api.command.CommandHandler;
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
    public String supportType() {
        return CockpitCommandType.DEPLOY_MODEL.name();
    }

    @Override
    public Single<DeployModelReply> handle(DeployModelCommand command) {
        DeployModelCommandPayload payload = command.getPayload();

        String apiCrossId = payload.modelId();
        String userId = payload.userId();
        String swaggerDefinition = payload.swaggerDefinition();
        String environmentId = payload.environmentId();
        DeploymentMode mode = DeploymentMode.fromDeployModelPayload(payload);
        List<String> labels = payload.labels();

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
                    return Single.just(new DeployModelReply(command.getId(), message.get()));
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
                    return Single.just(new DeployModelReply(command.getId(), message.get()));
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
                return Single.just(new DeployModelReply(command.getId()));
            }
            String errorDetails = "Failed to import API [%s].".formatted(result.getErrorMessage());
            logger.error(errorDetails);
            return Single.just(new DeployModelReply(command.getId(), errorDetails));
        } catch (Exception e) {
            String errorDetails = "Error occurred when importing api [%s]".formatted(payload.modelId());
            logger.error(errorDetails, e);
            return Single.just(new DeployModelReply(command.getId(), errorDetails));
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
