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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.designer.DeployModelPayload;
import io.gravitee.cockpit.api.command.designer.DeployModelReply;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntityResult;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.cockpit.services.ApiServiceCockpit;
import io.gravitee.rest.api.service.cockpit.services.CockpitApiPermissionChecker;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.observers.TestObserver;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(GraviteeContext.class)
@PowerMockIgnore({ "javax.security.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class DeployModelCommandHandlerTest {

    public static final String ENVIRONMENT_ID = "environment#id";
    public static final String ORGANIZATION_ID = "organization#id";

    @Mock
    private UserService userService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiServiceCockpit cockpitApiService;

    @Mock
    private CockpitApiPermissionChecker permissionChecker;

    @Mock
    private EnvironmentService environmentService;

    private DeployModelCommandHandler cut;

    @Before
    public void setUp() throws Exception {
        cut = new DeployModelCommandHandler(apiService, cockpitApiService, permissionChecker, userService, environmentService);
        PowerMockito.spy(GraviteeContext.class);
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.DEPLOY_MODEL_COMMAND, cut.handleType());
    }

    @Test
    public void creates_an_API_DOCUMENTED() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        environment.setOrganizationId(ORGANIZATION_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkCreatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                DeploymentMode.API_DOCUMENTED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                any(),
                eq(payload.getModelId()),
                eq(user.getId()),
                eq(payload.getSwaggerDefinition()),
                eq(environment.getId()),
                eq(DeploymentMode.API_DOCUMENTED),
                eq(payload.getLabels())
            )
        )
            .thenAnswer(
                i -> {
                    ApiEntity apiEntity = new ApiEntity();
                    apiEntity.setId(i.getArgument(1));
                    return ApiEntityResult.success(apiEntity);
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        PowerMockito.verifyStatic(GraviteeContext.class);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @Test
    public void creates_an_API_MOCKED_mode() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_MOCKED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkCreatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                DeploymentMode.API_MOCKED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                any(),
                eq(payload.getModelId()),
                eq(user.getId()),
                eq(payload.getSwaggerDefinition()),
                eq(environment.getId()),
                eq(DeploymentMode.API_MOCKED),
                eq(payload.getLabels())
            )
        )
            .thenAnswer(
                i -> {
                    ApiEntity apiEntity = new ApiEntity();
                    apiEntity.setId(i.getArgument(1));
                    return ApiEntityResult.success(apiEntity);
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void creates_an_API_PUBLISHED_mode() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_PUBLISHED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkCreatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                DeploymentMode.API_PUBLISHED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                any(),
                eq(payload.getModelId()),
                eq(user.getId()),
                eq(payload.getSwaggerDefinition()),
                eq(environment.getId()),
                eq(DeploymentMode.API_PUBLISHED),
                eq(payload.getLabels())
            )
        )
            .thenAnswer(
                i -> {
                    ApiEntity apiEntity = new ApiEntity();
                    apiEntity.setId(i.getArgument(1));
                    return ApiEntityResult.success(apiEntity);
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void updates_an_API_DOCUMENTED() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setEnvironmentId("env#id");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api#id");
        when(apiService.findByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.getModelId())).thenReturn(Optional.of(apiEntity));

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkUpdatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                apiEntity.getId(),
                DeploymentMode.API_DOCUMENTED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                any(),
                eq(apiEntity.getId()),
                eq(user.getId()),
                eq(payload.getSwaggerDefinition()),
                eq(environment.getId()),
                eq(DeploymentMode.API_DOCUMENTED),
                eq(payload.getLabels())
            )
        )
            .thenAnswer(
                i -> {
                    ApiEntity result = new ApiEntity();
                    result.setId(i.getArgument(1));
                    return ApiEntityResult.success(result);
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void updates_an_API_MOCKED_mode() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setEnvironmentId("env#id");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_MOCKED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api#id");
        when(apiService.findByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.getModelId())).thenReturn(Optional.of(apiEntity));

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkUpdatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                apiEntity.getId(),
                DeploymentMode.API_MOCKED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                any(),
                eq(apiEntity.getId()),
                eq(user.getId()),
                eq(payload.getSwaggerDefinition()),
                eq(environment.getId()),
                eq(DeploymentMode.API_MOCKED),
                eq(payload.getLabels())
            )
        )
            .thenAnswer(
                i -> {
                    ApiEntity result = new ApiEntity();
                    result.setId(i.getArgument(1));
                    return ApiEntityResult.success(result);
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void updates_an_API_PUBLISHED_mode() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setEnvironmentId("env#id");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_PUBLISHED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api#id");
        when(apiService.findByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.getModelId())).thenReturn(Optional.of(apiEntity));

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkUpdatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                apiEntity.getId(),
                DeploymentMode.API_PUBLISHED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.updateApi(
                any(),
                eq(apiEntity.getId()),
                eq(user.getId()),
                eq(payload.getSwaggerDefinition()),
                eq(environment.getId()),
                eq(DeploymentMode.API_PUBLISHED),
                eq(payload.getLabels())
            )
        )
            .thenAnswer(
                i -> {
                    ApiEntity result = new ApiEntity();
                    result.setId(i.getArgument(1));
                    return ApiEntityResult.success(result);
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handle_null_mode() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");
        payload.setMode(null);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkCreatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                DeploymentMode.API_DOCUMENTED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                any(),
                eq(payload.getModelId()),
                eq(user.getId()),
                eq(payload.getSwaggerDefinition()),
                eq(environment.getId()),
                eq(DeploymentMode.API_DOCUMENTED),
                eq(payload.getLabels())
            )
        )
            .thenAnswer(
                i -> {
                    ApiEntity apiEntity = new ApiEntity();
                    apiEntity.setId(i.getArgument(1));
                    return ApiEntityResult.success(apiEntity);
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithException() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkCreatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                DeploymentMode.API_DOCUMENTED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                GraviteeContext.getExecutionContext(),
                payload.getModelId(),
                payload.getUserId(),
                payload.getSwaggerDefinition(),
                environment.getId(),
                DeploymentMode.API_DOCUMENTED,
                payload.getLabels()
            )
        )
            .thenThrow(new RuntimeException("fake error"));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void fails_to_create_due_to_permission_issues() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(permissionChecker.checkCreatePermission(any(), eq(user.getId()), eq(environment.getId()), eq(DeploymentMode.API_DOCUMENTED)))
            .thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(
            reply -> {
                Assertions
                    .assertThat(reply)
                    .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getMessage)
                    .containsExactly(command.getId(), CommandStatus.FAILED, "You are not allowed to create APIs on this environment.");
                return true;
            }
        );
    }

    @Test
    public void fails_to_update_due_to_permission_issues() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setEnvironmentId("env#id");
        payload.setUserId("user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api#id");
        when(apiService.findByEnvironmentIdAndCrossId(ENVIRONMENT_ID, payload.getModelId())).thenReturn(Optional.of(apiEntity));

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkUpdatePermission(
                any(),
                eq(user.getId()),
                eq(environment.getId()),
                eq(apiEntity.getId()),
                eq(DeploymentMode.API_DOCUMENTED)
            )
        )
            .thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(
            reply -> {
                Assertions
                    .assertThat(reply)
                    .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getMessage)
                    .containsExactly(command.getId(), CommandStatus.FAILED, "You are not allowed to create APIs on this environment.");
                return true;
            }
        );
    }

    @Test
    public void clean_gravitee_context_on_success() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkCreatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                DeploymentMode.API_DOCUMENTED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                GraviteeContext.getExecutionContext(),
                payload.getModelId(),
                user.getId(),
                payload.getSwaggerDefinition(),
                environment.getId(),
                DeploymentMode.API_DOCUMENTED,
                payload.getLabels()
            )
        )
            .thenAnswer(
                i -> {
                    ApiEntity apiEntity = new ApiEntity();
                    apiEntity.setId(i.getArgument(0));
                    return ApiEntityResult.success(apiEntity);
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();
        obs.awaitTerminalEvent();
        obs.assertNoErrors();

        PowerMockito.verifyStatic(GraviteeContext.class);
        GraviteeContext.cleanContext();
    }

    @Test
    public void clean_gravitee_context_on_error() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkCreatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                DeploymentMode.API_DOCUMENTED
            )
        )
            .thenReturn(Optional.of("You are not allowed to create APIs on this environment."));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();

        PowerMockito.verifyStatic(GraviteeContext.class);
        GraviteeContext.cleanContext();
    }

    @Test
    public void fails_to_create_due_to_context_path_already_used() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");
        payload.setMode(DeployModelPayload.DeploymentMode.API_DOCUMENTED);
        payload.setLabels(List.of("label1", "label2"));

        DeployModelCommand command = new DeployModelCommand(payload);

        when(apiService.exists(payload.getModelId())).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource(any(), eq("cockpit"), eq(payload.getUserId()), eq(true))).thenReturn(user);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT_ID);
        when(environmentService.findByCockpitId(payload.getEnvironmentId())).thenReturn(environment);

        when(
            permissionChecker.checkCreatePermission(
                GraviteeContext.getExecutionContext(),
                user.getId(),
                environment.getId(),
                DeploymentMode.API_DOCUMENTED
            )
        )
            .thenReturn(Optional.empty());

        when(
            cockpitApiService.createApi(
                any(),
                eq(payload.getModelId()),
                eq(user.getId()),
                eq(payload.getSwaggerDefinition()),
                eq(environment.getId()),
                eq(DeploymentMode.API_DOCUMENTED),
                eq(payload.getLabels())
            )
        )
            .thenReturn(ApiEntityResult.failure("context path not available"));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(
            reply -> {
                Assertions
                    .assertThat(reply)
                    .extracting(DeployModelReply::getCommandId, DeployModelReply::getCommandStatus, DeployModelReply::getMessage)
                    .containsExactly(command.getId(), CommandStatus.FAILED, "context path not available");
                return true;
            }
        );
    }
}
