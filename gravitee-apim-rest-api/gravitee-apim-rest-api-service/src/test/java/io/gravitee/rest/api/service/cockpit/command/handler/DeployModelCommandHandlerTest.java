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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelCommandPayload;
import io.gravitee.cockpit.api.command.v1.designer.DeployModelReply;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntityResult;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.cockpit.services.ApiServiceCockpit;
import io.gravitee.rest.api.service.cockpit.services.CockpitApiPermissionChecker;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DeployModelCommandHandlerTest {

    public static final String ENVIRONMENT_ID = "environment#id";
    public static final String ORGANIZATION_ID = "organization#id";
    public static final String COCKPIT_ORGANIZATION_ID = "cockpit#organization#id";
    public static final String COCKPIT_ENVIRONMENT_ID = "cockpit#environment#id";
    public static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    @Mock
    private UserService userService;

    @Mock
    private ApiSearchService apiSearchService;

    private ApiServiceCockpit cockpitApiService;

    @Mock
    private CockpitApiPermissionChecker permissionChecker;

    @Mock
    private EnvironmentService environmentService;

    private DeployModelCommandHandler cut;

    @Before
    public void setUp() throws Exception {
        cockpitApiService = mock(ApiServiceCockpit.class, withSettings().verboseLogging());
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
            null,
            null
        );
    }

    @Test
    public void updates_an_API_DOCUMENTED() {
        DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        String apiId = "api#id";
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.modelId())).thenReturn(Optional.of(apiId));
        when(apiSearchService.findById(EXECUTION_CONTEXT, apiId)).thenReturn(
            io.gravitee.rest.api.model.v4.api.ApiEntity.builder().definitionVersion(DefinitionVersion.V2).build()
        );

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.userId()), eq(true))).thenReturn(user);

        when(
            permissionChecker.checkUpdatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, apiId, DeploymentMode.API_DOCUMENTED)
        ).thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                EXECUTION_CONTEXT,
                apiId,
                user.getId(),
                payload.swaggerDefinition(),
                ENVIRONMENT_ID,
                DeploymentMode.API_DOCUMENTED,
                payload.labels()
            )
        ).thenAnswer(i -> {
            ApiEntity result = new ApiEntity();
            result.setId(i.getArgument(1));
            return ApiEntityResult.success(result);
        });

        cut
            .handle(command)
            .test()
            .assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void updates_an_API_MOCKED_mode() {
        DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_MOCKED);

        DeployModelCommand command = new DeployModelCommand(payload);

        String apiId = "api#id";
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.modelId())).thenReturn(Optional.of(apiId));
        when(apiSearchService.findById(EXECUTION_CONTEXT, apiId)).thenReturn(
            io.gravitee.rest.api.model.v4.api.ApiEntity.builder().definitionVersion(DefinitionVersion.V2).build()
        );

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.userId()), eq(true))).thenReturn(user);

        when(
            permissionChecker.checkUpdatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, apiId, DeploymentMode.API_MOCKED)
        ).thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                EXECUTION_CONTEXT,
                apiId,
                user.getId(),
                payload.swaggerDefinition(),
                ENVIRONMENT_ID,
                DeploymentMode.API_MOCKED,
                payload.labels()
            )
        ).thenAnswer(i -> {
            ApiEntity result = new ApiEntity();
            result.setId(i.getArgument(1));
            return ApiEntityResult.success(result);
        });

        cut
            .handle(command)
            .test()
            .assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void updates_an_API_PUBLISHED_mode() {
        DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_PUBLISHED);

        DeployModelCommand command = new DeployModelCommand(payload);

        String apiId = "api#id";
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.modelId())).thenReturn(Optional.of(apiId));
        when(apiSearchService.findById(EXECUTION_CONTEXT, apiId)).thenReturn(
            io.gravitee.rest.api.model.v4.api.ApiEntity.builder().definitionVersion(DefinitionVersion.V2).build()
        );

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.userId(), true)).thenReturn(user);

        when(
            permissionChecker.checkUpdatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, apiId, DeploymentMode.API_PUBLISHED)
        ).thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                EXECUTION_CONTEXT,
                apiId,
                user.getId(),
                payload.swaggerDefinition(),
                ENVIRONMENT_ID,
                DeploymentMode.API_PUBLISHED,
                payload.labels()
            )
        ).thenAnswer(i -> {
            ApiEntity result = new ApiEntity();
            result.setId(i.getArgument(1));
            return ApiEntityResult.success(result);
        });

        cut
            .handle(command)
            .test()
            .assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.DEPLOY_MODEL.name(), cut.supportType());
    }

    @Test
    public void handleWithException() {
        DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.userId(), true)).thenReturn(user);

        when(
            permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED)
        ).thenReturn(Optional.empty());

        cut
            .handle(command)
            .test()
            .assertNoErrors()
            .assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void fails_to_create_due_to_permission_issues() {
        DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.userId(), true)).thenReturn(user);

        when(
            permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED)
        ).thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        cut
            .handle(command)
            .test()
            .assertNoErrors()
            .assertValue(reply -> {
                Assertions.assertThat(reply)
                    .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getErrorDetails)
                    .containsExactly(command.getId(), CommandStatus.ERROR, "You are not allowed to create APIs on this environment.");
                return true;
            });
    }

    @Test
    public void fails_to_update_due_to_permission_issues() {
        DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        String apiId = "api#id";
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.modelId())).thenReturn(Optional.of(apiId));

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.userId(), true)).thenReturn(user);

        when(
            permissionChecker.checkUpdatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, apiId, DeploymentMode.API_DOCUMENTED)
        ).thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        cut
            .handle(command)
            .test()
            .assertNoErrors()
            .assertValue(reply -> {
                Assertions.assertThat(reply)
                    .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getErrorDetails)
                    .containsExactly(command.getId(), CommandStatus.ERROR, "You are not allowed to create APIs on this environment.");
                return true;
            });
    }

    @Test
    public void clean_gravitee_context_on_success() {
        DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.userId(), true)).thenReturn(user);

        when(
            permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED)
        ).thenReturn(Optional.empty());

        cut.handle(command).test().assertNoErrors();
    }

    @Test
    public void clean_gravitee_context_on_error() {
        DeployModelCommandPayload payload = createDeployPayload(DeployModelCommandPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.userId(), true)).thenReturn(user);

        when(
            permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED)
        ).thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        cut.handle(command).test().assertNoErrors();
    }

    private static DeployModelCommandPayload createDeployPayload(final DeployModelCommandPayload.DeploymentMode deploymentMode) {
        return DeployModelCommandPayload.builder()
            .modelId("model#1")
            .swaggerDefinition("swagger-definition")
            .userId("cockpit_user#id")
            .mode(deploymentMode)
            .labels(List.of("label1", "label2"))
            .organizationId(DeployModelCommandHandlerTest.COCKPIT_ORGANIZATION_ID)
            .environmentId(DeployModelCommandHandlerTest.COCKPIT_ENVIRONMENT_ID)
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
