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
package io.gravitee.rest.api.service.impl.commands;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.environment.EnvironmentCommand;
import io.gravitee.cockpit.api.command.environment.EnvironmentPayload;
import io.gravitee.cockpit.api.command.environment.EnvironmentReply;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentCommandHandlerTest {

    @Mock
    private EnvironmentService environmentService;

    public EnvironmentCommandHandler cut;

    @Before
    public void before() {
        cut = new EnvironmentCommandHandler(environmentService);
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.ENVIRONMENT_COMMAND, cut.handleType());
    }

    @Test
    public void handle() {

        EnvironmentPayload environmentPayload = new EnvironmentPayload();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);

        environmentPayload.setId("env#1");
        environmentPayload.setHrids(Collections.singletonList("env-1"));
        environmentPayload.setOrganizationId("orga#1");
        environmentPayload.setDescription("Environment description");
        environmentPayload.setName("Environment name");
        environmentPayload.setDomainRestrictions(Arrays.asList("domain.restriction1.io", "domain.restriction2.io"));

        when(environmentService.createOrUpdate(eq("orga#1"), eq("env#1"),
                argThat(newEnvironment -> newEnvironment.getHrids().equals(environmentPayload.getHrids())
                        && newEnvironment.getDescription().equals(environmentPayload.getDescription())
                        && newEnvironment.getName().equals(environmentPayload.getName())
                        && newEnvironment.getDomainRestrictions().equals(environmentPayload.getDomainRestrictions()))))
                .thenReturn(new EnvironmentEntity());

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithException() {

        EnvironmentPayload environmentPayload = new EnvironmentPayload();
        EnvironmentCommand command = new EnvironmentCommand(environmentPayload);

        environmentPayload.setId("env#1");
        environmentPayload.setOrganizationId("orga#1");
        environmentPayload.setDescription("Environment description");
        environmentPayload.setName("Environment name");
        environmentPayload.setDomainRestrictions(Arrays.asList("domain.restriction1.io", "domain.restriction2.io"));

        when(environmentService.createOrUpdate(eq("orga#1"), eq("env#1"), any(UpdateEnvironmentEntity.class))).thenThrow(new RuntimeException("fake error"));

        TestObserver<EnvironmentReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }
}