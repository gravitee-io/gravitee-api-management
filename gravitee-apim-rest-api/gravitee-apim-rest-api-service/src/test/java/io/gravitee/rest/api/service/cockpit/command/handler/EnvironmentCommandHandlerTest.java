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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.portal.domain_service.DefaultPortalDomainService;
import io.gravitee.apim.core.portal_page.use_case.CreateDefaultPortalNavigationItemsUseCase;
import io.gravitee.cockpit.api.command.model.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentCommand;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentCommandPayload;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class EnvironmentCommandHandlerTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private AccessPointCrudService accessPointService;

    @Mock
    private CreateDefaultPortalNavigationItemsUseCase createDefaultPortalNavigationItemsUseCase;

    @Mock
    private DefaultPortalDomainService defaultPortalDomainService;

    public EnvironmentCommandHandler cut;

    @BeforeEach
    public void before() {
        cut = new EnvironmentCommandHandler(
            environmentService,
            accessPointService,
            createDefaultPortalNavigationItemsUseCase,
            defaultPortalDomainService
        );
    }

    @AfterEach
    void cleanGraviteeContext() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.ENVIRONMENT.name(), cut.supportType());
    }

    @Test
    @SneakyThrows
    public void handle() throws InterruptedException {
        String envId = "env#1";
        String orgId = "orga#1";
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload.builder()
            .id(envId)
            .cockpitId("env#cockpit-1")
            .hrids(Collections.singletonList("env-1"))
            .organizationId(orgId)
            .description("Environment description")
            .name("Environment name")
            .accessPoints(
                List.of(
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction1.io").build(),
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction2.io").build()
                )
            )
            .build();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);

        when(environmentService.findByCockpitId(any())).thenThrow(new EnvironmentNotFoundException("Env not found"));
        when(
            environmentService.createOrUpdate(
                eq(orgId),
                eq(envId),
                argThat(
                    newEnvironment ->
                        newEnvironment.getCockpitId().equals(environmentPayload.cockpitId()) &&
                        newEnvironment.getHrids().equals(environmentPayload.hrids()) &&
                        newEnvironment.getDescription().equals(environmentPayload.description()) &&
                        newEnvironment.getName().equals(environmentPayload.name())
                )
            )
        ).thenReturn(EnvironmentEntity.builder().id(envId).organizationId(orgId).build());

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    @SneakyThrows
    void should_not_resolve_an_environment_portal_url_with_the_one_memoized_for_a_previous_environment() {
        // ParameterService memoizes portal.url per thread by key only: simulate the value read for env X
        doAnswer(invocation -> GraviteeContext.getCurrentParameters().put(Key.PORTAL_URL, "https://x.acme.io"))
            .when(defaultPortalDomainService)
            .syncUrl("env-x");
        var memoizedWhenHandlingY = new AtomicReference<String>();
        doAnswer(invocation -> {
            memoizedWhenHandlingY.set(GraviteeContext.getCurrentParameters().get(Key.PORTAL_URL));
            return Optional.empty();
        })
            .when(defaultPortalDomainService)
            .ensureDefault("orga#1", "env-y");
        when(environmentService.findByCockpitId(any())).thenThrow(new EnvironmentNotFoundException("Env not found"));
        when(environmentService.createOrUpdate(eq("orga#1"), any(), any())).thenAnswer(invocation ->
            EnvironmentEntity.builder().id(invocation.getArgument(1)).organizationId("orga#1").build()
        );

        cut.handle(new EnvironmentCommand(anEnvironmentPayload("env-x"))).test().await();
        cut.handle(new EnvironmentCommand(anEnvironmentPayload("env-y"))).test().await();

        assertThat(memoizedWhenHandlingY.get()).isNull();
    }

    private static EnvironmentCommandPayload anEnvironmentPayload(String envId) {
        return EnvironmentCommandPayload.builder()
            .id(envId)
            .cockpitId("cockpit-" + envId)
            .hrids(Collections.singletonList(envId))
            .organizationId("orga#1")
            .name(envId)
            .build();
    }

    @Test
    @SneakyThrows
    void should_ensure_default_portal_and_sync_url_after_updating_access_points() {
        String envId = "env#1";
        String orgId = "orga#1";
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload.builder()
            .id(envId)
            .cockpitId("env#cockpit-1")
            .hrids(Collections.singletonList("env-1"))
            .organizationId(orgId)
            .name("Environment name")
            .accessPoints(List.of(AccessPoint.builder().target(AccessPoint.Target.PORTAL).host("portal.acme.io").build()))
            .build();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);
        when(environmentService.findByCockpitId(any())).thenThrow(new EnvironmentNotFoundException("Env not found"));
        when(environmentService.createOrUpdate(eq(orgId), eq(envId), any())).thenReturn(
            EnvironmentEntity.builder().id(envId).organizationId(orgId).build()
        );

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
        var inOrder = inOrder(accessPointService, defaultPortalDomainService);
        inOrder.verify(accessPointService).updateAccessPoints(any(), eq(envId), any());
        inOrder.verify(defaultPortalDomainService).ensureDefault(orgId, envId);
        inOrder.verify(defaultPortalDomainService).syncUrl(envId);
    }

    @Test
    public void handleWithException() throws InterruptedException {
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload.builder()
            .id("env#1")
            .cockpitId("env#cockpit-1")
            .hrids(Collections.singletonList("env-1"))
            .organizationId("orga#1")
            .description("Environment description")
            .name("Environment name")
            .accessPoints(
                List.of(
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction1.io").build(),
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction2.io").build()
                )
            )
            .build();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);

        when(environmentService.findByCockpitId(any())).thenThrow(new EnvironmentNotFoundException("Env not found"));
        when(environmentService.createOrUpdate(eq("orga#1"), eq("env#1"), any(UpdateEnvironmentEntity.class))).thenThrow(
            new RuntimeException("fake error")
        );

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void handleWithExistingCockpitId() throws InterruptedException {
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload.builder()
            .id("env#1")
            .cockpitId("env#cockpit-1")
            .hrids(Collections.singletonList("env-1"))
            .organizationId("orga#1")
            .description("Environment description")
            .name("Environment name")
            .accessPoints(
                List.of(
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction1.io").build(),
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction2.io").build()
                )
            )
            .build();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);
        EnvironmentEntity existingEnvironment = mock(EnvironmentEntity.class);
        when(existingEnvironment.getId()).thenReturn("DEFAULT");
        when(existingEnvironment.getOrganizationId()).thenReturn("DEFAULT");
        when(environmentService.findByCockpitId(any())).thenReturn(existingEnvironment);
        when(
            environmentService.createOrUpdate(
                eq("DEFAULT"),
                eq("DEFAULT"),
                argThat(
                    newEnvironment ->
                        newEnvironment.getCockpitId().equals(environmentPayload.cockpitId()) &&
                        newEnvironment.getHrids().equals(environmentPayload.hrids()) &&
                        newEnvironment.getDescription().equals(environmentPayload.description()) &&
                        newEnvironment.getName().equals(environmentPayload.name())
                )
            )
        ).thenReturn(new EnvironmentEntity());

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    @SneakyThrows
    public void environmentCreationCreatesDefaultAssets() {
        String envId = "env#1";
        String orgId = "orga#1";
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload.builder()
            .id(envId)
            .cockpitId("env#cockpit-1")
            .hrids(Collections.singletonList("env-1"))
            .organizationId(orgId)
            .description("Environment description")
            .name("Environment name")
            .build();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);

        when(environmentService.findByCockpitId(any())).thenThrow(new EnvironmentNotFoundException("Env not found"));
        when(environmentService.createOrUpdate(eq(orgId), eq(envId), any(UpdateEnvironmentEntity.class))).thenReturn(
            EnvironmentEntity.builder().id(envId).organizationId(orgId).build()
        );

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        verify(createDefaultPortalNavigationItemsUseCase).execute(orgId, envId);
    }

    @Test
    @SneakyThrows
    public void environmentCreationCreatesDefaultPortal() {
        String envId = "env#1";
        String orgId = "orga#1";
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload.builder()
            .id(envId)
            .cockpitId("env#cockpit-1")
            .hrids(Collections.singletonList("env-1"))
            .organizationId(orgId)
            .description("Environment description")
            .name("Environment name")
            .build();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);

        when(environmentService.findByCockpitId(any())).thenThrow(new EnvironmentNotFoundException("Env not found"));
        when(environmentService.createOrUpdate(eq(orgId), eq(envId), any(UpdateEnvironmentEntity.class))).thenReturn(
            EnvironmentEntity.builder().id(envId).organizationId(orgId).build()
        );

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void environmentUpdateDoesNotCreateDefaultPortal() throws InterruptedException {
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload.builder()
            .id("env#1")
            .cockpitId("env#cockpit-1")
            .hrids(Collections.singletonList("env-1"))
            .organizationId("orga#1")
            .description("Environment description")
            .name("Environment name")
            .build();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);
        EnvironmentEntity existingEnvironment = mock(EnvironmentEntity.class);
        when(existingEnvironment.getId()).thenReturn("DEFAULT");
        when(existingEnvironment.getOrganizationId()).thenReturn("DEFAULT");
        when(environmentService.findByCockpitId(any())).thenReturn(existingEnvironment);
        when(
            environmentService.createOrUpdate(
                eq("DEFAULT"),
                eq("DEFAULT"),
                argThat(
                    newEnvironment ->
                        newEnvironment.getCockpitId().equals(environmentPayload.cockpitId()) &&
                        newEnvironment.getHrids().equals(environmentPayload.hrids()) &&
                        newEnvironment.getDescription().equals(environmentPayload.description()) &&
                        newEnvironment.getName().equals(environmentPayload.name())
                )
            )
        ).thenReturn(new EnvironmentEntity());

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void environmentUpdateDoesNotCreateDefaultPortalNavigationItems() throws InterruptedException {
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload.builder()
            .id("env#1")
            .cockpitId("env#cockpit-1")
            .hrids(Collections.singletonList("env-1"))
            .organizationId("orga#1")
            .description("Environment description")
            .name("Environment name")
            .build();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);
        EnvironmentEntity existingEnvironment = mock(EnvironmentEntity.class);
        when(existingEnvironment.getId()).thenReturn("DEFAULT");
        when(existingEnvironment.getOrganizationId()).thenReturn("DEFAULT");
        when(environmentService.findByCockpitId(any())).thenReturn(existingEnvironment);
        when(
            environmentService.createOrUpdate(
                eq("DEFAULT"),
                eq("DEFAULT"),
                argThat(
                    newEnvironment ->
                        newEnvironment.getCockpitId().equals(environmentPayload.cockpitId()) &&
                        newEnvironment.getHrids().equals(environmentPayload.hrids()) &&
                        newEnvironment.getDescription().equals(environmentPayload.description()) &&
                        newEnvironment.getName().equals(environmentPayload.name())
                )
            )
        ).thenReturn(new EnvironmentEntity());

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        verify(createDefaultPortalNavigationItemsUseCase, never()).execute(any(), any());
    }
}
