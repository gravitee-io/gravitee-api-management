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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.cockpit.api.command.model.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentCommand;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentCommandPayload;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentCommandHandlerTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private AccessPointCrudService accessPointService;

    public EnvironmentCommandHandler cut;

    @Before
    public void before() {
        cut = new EnvironmentCommandHandler(environmentService, accessPointService);
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.ENVIRONMENT.name(), cut.supportType());
    }

    @Test
    public void handle() throws InterruptedException {
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload
            .builder()
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
        when(
            environmentService.createOrUpdate(
                eq("orga#1"),
                eq("env#1"),
                argThat(newEnvironment ->
                    newEnvironment.getCockpitId().equals(environmentPayload.cockpitId()) &&
                    newEnvironment.getHrids().equals(environmentPayload.hrids()) &&
                    newEnvironment.getDescription().equals(environmentPayload.description()) &&
                    newEnvironment.getName().equals(environmentPayload.name())
                )
            )
        )
            .thenReturn(new EnvironmentEntity());

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithException() throws InterruptedException {
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload
            .builder()
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
        when(environmentService.createOrUpdate(eq("orga#1"), eq("env#1"), any(UpdateEnvironmentEntity.class)))
            .thenThrow(new RuntimeException("fake error"));

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void handleWithExistingCockpitId() throws InterruptedException {
        EnvironmentCommandPayload environmentPayload = EnvironmentCommandPayload
            .builder()
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
                argThat(newEnvironment ->
                    newEnvironment.getCockpitId().equals(environmentPayload.cockpitId()) &&
                    newEnvironment.getHrids().equals(environmentPayload.hrids()) &&
                    newEnvironment.getDescription().equals(environmentPayload.description()) &&
                    newEnvironment.getName().equals(environmentPayload.name())
                )
            )
        )
            .thenReturn(new EnvironmentEntity());

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }
}
