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

import static org.mockito.Mockito.*;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.installation.*;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import io.reactivex.rxjava3.observers.TestObserver;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UnlinkInstallationCommandHandlerTest extends TestCase {

    private static final String COCKPIT_ORG_ID = "coc-org#1";
    private static final String COCKPIT_ENV_ID = "coc-env#1";

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private AccessPointCrudService accessPointCrudService;

    public UnlinkInstallationCommandHandler cut;

    @Before
    public void before() {
        cut = new UnlinkInstallationCommandHandler(organizationService, environmentService, accessPointCrudService);
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.UNLINK_INSTALLATION_COMMAND, cut.handleType());
    }

    @Test
    public void handle() throws InterruptedException {
        final UnlinkInstallationPayload payload = new UnlinkInstallationPayload();
        payload.setEnvironmentCockpitId(COCKPIT_ENV_ID);
        payload.setOrganizationCockpitId(COCKPIT_ORG_ID);

        EnvironmentEntity environment = mock(EnvironmentEntity.class);
        String envId = "env#1";
        String orgId = "org#1";
        when(environment.getId()).thenReturn(envId);
        OrganizationEntity organization = mock(OrganizationEntity.class);
        when(organization.getId()).thenReturn(orgId);

        when(environmentService.findByCockpitId(COCKPIT_ENV_ID)).thenReturn(environment);
        when(organizationService.findByCockpitId(COCKPIT_ORG_ID)).thenReturn(organization);

        UnlinkInstallationCommand command = new UnlinkInstallationCommand(payload);

        TestObserver<UnlinkInstallationReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        verify(accessPointCrudService, times(1)).deleteAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, envId);
        verify(accessPointCrudService, times(1)).deleteAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, orgId);
    }

    @Test
    public void handleWithException() throws InterruptedException {
        final UnlinkInstallationPayload payload = new UnlinkInstallationPayload();
        payload.setEnvironmentCockpitId(COCKPIT_ENV_ID);

        UnlinkInstallationCommand command = new UnlinkInstallationCommand(payload);

        TestObserver<UnlinkInstallationReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }
}
