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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.designer.DeployModelPayload;
import io.gravitee.cockpit.api.command.designer.DeployModelReply;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntityResult;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.cockpit.services.ApiServiceCockpit;
import io.gravitee.rest.api.service.cockpit.services.CockpitApiPermissionChecker;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.reactivex.rxjava3.observers.TestObserver;
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
    public static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    @Mock
    private UserService userService;

    @Mock
    private ApiSearchService apiSearchService;

    private ApiServiceCockpit cockpitApiService;

    @Mock
    private CockpitApiPermissionChecker permissionChecker;

    private DeployModelCommandHandler cut;

    @Before
    public void setUp() throws Exception {
        cockpitApiService = mock(ApiServiceCockpit.class, withSettings().verboseLogging());
        cut = new DeployModelCommandHandler(apiSearchService, cockpitApiService, permissionChecker, userService);
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.DEPLOY_MODEL_COMMAND, cut.handleType());
    }

    @Test
    public void creates_an_API_DOCUMENTED() throws InterruptedException {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));
        payload.setEnvironmentId(ENVIRONMENT_ID);
        payload.setOrganizationId(ORGANIZATION_ID);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                EXECUTION_CONTEXT,
                payload.getModelId(),
                user.getId(),
                payload.getSwaggerDefinition(),
                payload.getEnvironmentId(),
                DeploymentMode.API_DOCUMENTED,
                payload.getLabels()
            )
        )
            .thenAnswer(i -> {
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId(i.getArgument(1));
                return ApiEntityResult.success(apiEntity);
            });

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void creates_an_API_MOCKED_mode() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_MOCKED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_MOCKED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                EXECUTION_CONTEXT,
                payload.getModelId(),
                user.getId(),
                payload.getSwaggerDefinition(),
                payload.getEnvironmentId(),
                DeploymentMode.API_MOCKED,
                payload.getLabels()
            )
        )
            .thenAnswer(i -> {
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId(i.getArgument(1));
                return ApiEntityResult.success(apiEntity);
            });

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void creates_an_API_PUBLISHED_mode() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_PUBLISHED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                EXECUTION_CONTEXT,
                payload.getModelId(),
                user.getId(),
                payload.getSwaggerDefinition(),
                payload.getEnvironmentId(),
                DeploymentMode.API_PUBLISHED,
                payload.getLabels()
            )
        )
            .thenAnswer(i -> {
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId(i.getArgument(1));
                return ApiEntityResult.success(apiEntity);
            });

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void updates_an_API_DOCUMENTED() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        String apiId = "api#id";
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.getModelId())).thenReturn(Optional.of(apiId));

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        when(permissionChecker.checkUpdatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, apiId, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                EXECUTION_CONTEXT,
                apiId,
                user.getId(),
                payload.getSwaggerDefinition(),
                payload.getEnvironmentId(),
                DeploymentMode.API_DOCUMENTED,
                payload.getLabels()
            )
        )
            .thenAnswer(i -> {
                ApiEntity result = new ApiEntity();
                result.setId(i.getArgument(1));
                return ApiEntityResult.success(result);
            });

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void updates_an_API_MOCKED_mode() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_MOCKED);

        DeployModelCommand command = new DeployModelCommand(payload);

        String apiId = "api#id";
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.getModelId())).thenReturn(Optional.of(apiId));

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        when(permissionChecker.checkUpdatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, apiId, DeploymentMode.API_MOCKED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                EXECUTION_CONTEXT,
                apiId,
                user.getId(),
                payload.getSwaggerDefinition(),
                payload.getEnvironmentId(),
                DeploymentMode.API_MOCKED,
                payload.getLabels()
            )
        )
            .thenAnswer(i -> {
                ApiEntity result = new ApiEntity();
                result.setId(i.getArgument(1));
                return ApiEntityResult.success(result);
            });

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void updates_an_API_PUBLISHED_mode() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_PUBLISHED);

        DeployModelCommand command = new DeployModelCommand(payload);

        String apiId = "api#id";
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.getModelId())).thenReturn(Optional.of(apiId));

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkUpdatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, apiId, DeploymentMode.API_PUBLISHED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                EXECUTION_CONTEXT,
                apiId,
                user.getId(),
                payload.getSwaggerDefinition(),
                payload.getEnvironmentId(),
                DeploymentMode.API_PUBLISHED,
                payload.getLabels()
            )
        )
            .thenAnswer(i -> {
                ApiEntity result = new ApiEntity();
                result.setId(i.getArgument(1));
                return ApiEntityResult.success(result);
            });

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handle_null_mode() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(null);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                EXECUTION_CONTEXT,
                payload.getModelId(),
                user.getId(),
                payload.getSwaggerDefinition(),
                payload.getEnvironmentId(),
                DeploymentMode.API_DOCUMENTED,
                payload.getLabels()
            )
        )
            .thenAnswer(i -> {
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId(i.getArgument(1));
                return ApiEntityResult.success(apiEntity);
            });

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithException() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                EXECUTION_CONTEXT,
                payload.getModelId(),
                user.getId(),
                payload.getSwaggerDefinition(),
                payload.getEnvironmentId(),
                DeploymentMode.API_DOCUMENTED,
                payload.getLabels()
            )
        )
            .thenThrow(new RuntimeException("fake error"));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void fails_to_create_due_to_permission_issues() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> {
            Assertions
                .assertThat(reply)
                .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getMessage)
                .containsExactly(command.getId(), CommandStatus.FAILED, "You are not allowed to create APIs on this environment.");
            return true;
        });
    }

    @Test
    public void fails_to_update_due_to_permission_issues() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        String apiId = "api#id";
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.getModelId())).thenReturn(Optional.of(apiId));

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkUpdatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, apiId, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> {
            Assertions
                .assertThat(reply)
                .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getMessage)
                .containsExactly(command.getId(), CommandStatus.FAILED, "You are not allowed to create APIs on this environment.");
            return true;
        });
    }

    @Test
    public void clean_gravitee_context_on_success() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                EXECUTION_CONTEXT,
                payload.getModelId(),
                user.getId(),
                payload.getSwaggerDefinition(),
                ENVIRONMENT_ID,
                DeploymentMode.API_DOCUMENTED,
                payload.getLabels()
            )
        )
            .thenAnswer(i -> {
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId(i.getArgument(0));
                return ApiEntityResult.success(apiEntity);
            });

        TestObserver<DeployModelReply> obs = cut.handle(command).test();
        obs.await();
        obs.assertNoErrors();
    }

    @Test
    public void clean_gravitee_context_on_error() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
    }

    @Test
    public void fails_to_create_due_to_context_path_already_used() throws InterruptedException {
        DeployModelPayload payload = createDeployPayload(DeployModelPayload.DeploymentMode.API_DOCUMENTED);

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = createUserEntity(payload);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", payload.getUserId(), true)).thenReturn(user);

        when(permissionChecker.checkCreatePermission(EXECUTION_CONTEXT, user.getId(), ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED))
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                EXECUTION_CONTEXT,
                payload.getModelId(),
                user.getId(),
                payload.getSwaggerDefinition(),
                ENVIRONMENT_ID,
                DeploymentMode.API_DOCUMENTED,
                payload.getLabels()
            )
        )
            .thenReturn(ApiEntityResult.failure("context path not available"));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> {
            Assertions
                .assertThat(reply)
                .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getMessage)
                .containsExactly(command.getId(), CommandStatus.FAILED, "context path not available");
            return true;
        });
    }

    private static DeployModelPayload createDeployPayload(final DeployModelPayload.DeploymentMode deploymentMode) {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");
        payload.setMode(deploymentMode);
        payload.setLabels(List.of("label1", "label2"));
        payload.setEnvironmentId(ENVIRONMENT_ID);
        payload.setOrganizationId(ORGANIZATION_ID);
        return payload;
    }

    private static UserEntity createUserEntity(final DeployModelPayload payload) {
        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        user.setOrganizationId(ORGANIZATION_ID);
        return user;
    }
}
