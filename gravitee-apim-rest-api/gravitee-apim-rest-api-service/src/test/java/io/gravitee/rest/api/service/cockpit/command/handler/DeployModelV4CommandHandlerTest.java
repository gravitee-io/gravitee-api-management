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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.use_case.cockpit.DeployModelToApiCreateUseCase;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelCommandPayload;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.cockpit.services.ApiServiceCockpit;
import io.gravitee.rest.api.service.cockpit.services.CockpitApiPermissionChecker;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Aurélien PACAUD (aurelien.pacaud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class DeployModelV4CommandHandlerTest {

    public static final String ENVIRONMENT_ID = "environment#id";
    public static final String ORGANIZATION_ID = "organization#id";
    public static final String COCKPIT_ORGANIZATION_ID = "cockpit#organization#id";
    public static final String COCKPIT_ENVIRONMENT_ID = "cockpit#environment#id";
    public static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    @Mock
    private UserService userService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiServiceCockpit cockpitApiService;

    @Mock
    private CockpitApiPermissionChecker permissionChecker;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DeployModelToApiCreateUseCase deployModelToApiCreateUseCase;

    private DeployModelCommandHandler cut;

    @BeforeEach
    public void setUp() throws Exception {
        lenient()
            .when(environmentService.findByCockpitId(COCKPIT_ENVIRONMENT_ID))
            .thenReturn(
                EnvironmentEntity.builder().id(ENVIRONMENT_ID).organizationId(ORGANIZATION_ID).cockpitId(COCKPIT_ENVIRONMENT_ID).build()
            );
        cut = new DeployModelCommandHandler(
            apiSearchService,
            cockpitApiService,
            permissionChecker,
            userService,
            environmentService,
            deployModelToApiCreateUseCase
        );
    }

    @Nested
    public class Create {

        @ParameterizedTest
        @MethodSource
        public void create_API(DeployModelCommandPayload.DeploymentMode deploymentMode, DeploymentMode expectedPermissionMode) {
            var payload = createDeployPayload(deploymentMode);
            var command = new DeployModelCommand(payload);
            var user = createUserEntity(payload);

            when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.userId(), true)).thenReturn(user);
            when(
                permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, expectedPermissionMode)
            ).thenReturn(Optional.empty());

            when(deployModelToApiCreateUseCase.execute(any())).thenReturn(new DeployModelToApiCreateUseCase.Output(new ApiWithFlows()));

            cut
                .handle(command)
                .test()
                .assertValue(
                    reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED)
                );
        }

        static Stream<Arguments> create_API() {
            return Stream.of(
                Arguments.of(DeployModelCommandPayload.DeploymentMode.API_MOCKED, DeploymentMode.API_MOCKED),
                Arguments.of(DeployModelCommandPayload.DeploymentMode.API_PUBLISHED, DeploymentMode.API_PUBLISHED),
                Arguments.of(DeployModelCommandPayload.DeploymentMode.API_DOCUMENTED, DeploymentMode.API_DOCUMENTED)
            );
        }

        @Test
        public void fails_to_create_due_to_context_path_already_used() {
            DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_DOCUMENTED);

            DeployModelCommand command = new DeployModelCommand(payload);

            UserEntity user = createUserEntity(payload);
            when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.userId(), true)).thenReturn(user);

            when(
                permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED)
            ).thenReturn(Optional.empty());

            when(
                deployModelToApiCreateUseCase.execute(
                    argThat(
                        input ->
                            input.swaggerDefinition().equals(payload.swaggerDefinition()) &&
                            input.auditInfo().organizationId().equals("organization#id") &&
                            input.auditInfo().environmentId().equals(ENVIRONMENT_ID)
                    )
                )
            ).thenThrow(new InvalidPathsException("Path [/test/] already exists"));

            cut
                .handle(command)
                .test()
                .assertNoErrors()
                .assertValue(reply -> {
                    Assertions.assertThat(reply)
                        .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getErrorDetails)
                        .containsExactly(command.getId(), CommandStatus.ERROR, "Failed to import API [context path not available].");
                    return true;
                });
        }
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.DEPLOY_MODEL.name(), cut.supportType());
    }

    private static DeployModelCommandPayload createDeployPayload(final DeployModelCommandPayload.DeploymentMode deploymentMode) {
        return DeployModelCommandPayload.builder()
            .modelId("model#1")
            .swaggerDefinition("swagger-definition")
            .userId("cockpit_user#id")
            .mode(deploymentMode)
            .labels(List.of("label1", "label2"))
            .environmentId(DeployModelV4CommandHandlerTest.COCKPIT_ENVIRONMENT_ID)
            .organizationId(DeployModelV4CommandHandlerTest.COCKPIT_ORGANIZATION_ID)
            .build();
    }

    private static UserEntity createUserEntity(final DeployModelCommandPayload payload) {
        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.userId());
        user.setOrganizationId(ORGANIZATION_ID);
        return user;
    }
}
