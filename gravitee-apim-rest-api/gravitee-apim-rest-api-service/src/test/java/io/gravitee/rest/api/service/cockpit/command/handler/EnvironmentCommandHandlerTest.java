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
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.environment.EnvironmentCommand;
import io.gravitee.cockpit.api.command.environment.EnvironmentPayload;
import io.gravitee.cockpit.api.command.environment.EnvironmentReply;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
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
    public void handleType() {
        assertEquals(Command.Type.ENVIRONMENT_COMMAND, cut.handleType());
    }

    @Test
    public void handle() throws InterruptedException {
        EnvironmentPayload environmentPayload = new EnvironmentPayload();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);

        environmentPayload.setId("env#1");
        environmentPayload.setCockpitId("env#cockpit-1");
        environmentPayload.setHrids(Collections.singletonList("env-1"));
        environmentPayload.setOrganizationId("orga#1");
        environmentPayload.setDescription("Environment description");
        environmentPayload.setName("Environment name");
        environmentPayload.setAccessPoints(
            List.of(
                AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction1.io").build(),
                AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction2.io").build()
            )
        );

        when(
            environmentService.createOrUpdate(
                eq("orga#1"),
                eq("env#1"),
                argThat(newEnvironment ->
                    newEnvironment.getCockpitId().equals(environmentPayload.getCockpitId()) &&
                    newEnvironment.getHrids().equals(environmentPayload.getHrids()) &&
                    newEnvironment.getDescription().equals(environmentPayload.getDescription()) &&
                    newEnvironment.getName().equals(environmentPayload.getName())
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
        EnvironmentPayload environmentPayload = new EnvironmentPayload();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);

        environmentPayload.setId("env#1");
        environmentPayload.setCockpitId("env#cockpit-1");
        environmentPayload.setOrganizationId("orga#1");
        environmentPayload.setDescription("Environment description");
        environmentPayload.setName("Environment name");
        environmentPayload.setAccessPoints(
            List.of(
                AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction1.io").build(),
                AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction2.io").build()
            )
        );

        when(environmentService.createOrUpdate(eq("orga#1"), eq("env#1"), any(UpdateEnvironmentEntity.class)))
            .thenThrow(new RuntimeException("fake error"));

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }
}
