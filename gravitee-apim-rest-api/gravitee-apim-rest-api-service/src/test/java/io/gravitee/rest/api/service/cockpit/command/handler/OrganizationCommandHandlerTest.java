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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.cockpit.api.command.model.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.organization.OrganizationCommand;
import io.gravitee.cockpit.api.command.v1.organization.OrganizationCommandPayload;
import io.gravitee.cockpit.api.command.v1.organization.OrganizationReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
public class OrganizationCommandHandlerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private AccessPointCrudService accessPointService;

    @Mock
    private LicenseDomainService licenseService;

    @Mock
    public OrganizationCommandHandler cut;

    @Before
    public void before() {
        cut = new OrganizationCommandHandler(organizationService, accessPointService, licenseService);
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.ORGANIZATION.name(), cut.supportType());
    }

    @Test
    public void handle() throws InterruptedException {
        OrganizationCommandPayload organizationPayload = OrganizationCommandPayload.builder()
            .id("orga#1")
            .cockpitId("org#cockpit-1")
            .hrids(Collections.singletonList("orga-1"))
            .description("Organization description")
            .name("Organization name")
            .accessPoints(
                List.of(
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction1.io").build(),
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction2.io").build()
                )
            )
            .build();
        OrganizationCommand command = new OrganizationCommand(organizationPayload);

        when(organizationService.findByCockpitId(any())).thenThrow(new OrganizationNotFoundException("Org not found"));
        when(
            organizationService.createOrUpdate(
                argThat(orgaId -> orgaId.equals("orga#1")),
                argThat(
                    newOrganization ->
                        newOrganization.getCockpitId().equals(organizationPayload.cockpitId()) &&
                        newOrganization.getHrids().equals(organizationPayload.hrids()) &&
                        newOrganization.getDescription().equals(organizationPayload.description()) &&
                        newOrganization.getName().equals(organizationPayload.name())
                )
            )
        ).thenReturn(new OrganizationEntity());

        TestObserver<OrganizationReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithException() throws InterruptedException {
        OrganizationCommandPayload organizationPayload = OrganizationCommandPayload.builder()
            .id("orga#1")
            .description("Organization description")
            .name("Organization name")
            .accessPoints(
                List.of(
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction1.io").build(),
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction2.io").build()
                )
            )
            .build();
        OrganizationCommand command = new OrganizationCommand(organizationPayload);

        when(organizationService.findByCockpitId(any())).thenThrow(new OrganizationNotFoundException("Org not found"));
        when(organizationService.createOrUpdate(argThat(orgaId -> orgaId.equals("orga#1")), any(UpdateOrganizationEntity.class))).thenThrow(
            new RuntimeException("fake error")
        );

        TestObserver<OrganizationReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void handleWithExistingCockpitId() throws InterruptedException {
        OrganizationCommandPayload organizationPayload = OrganizationCommandPayload.builder()
            .id("orga#1")
            .cockpitId("org#cockpit-1")
            .hrids(Collections.singletonList("orga-1"))
            .description("Organization description")
            .name("Organization name")
            .accessPoints(
                List.of(
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction1.io").build(),
                    AccessPoint.builder().target(AccessPoint.Target.CONSOLE).host("domain.restriction2.io").build()
                )
            )
            .build();
        OrganizationCommand command = new OrganizationCommand(organizationPayload);

        OrganizationEntity existingOrganization = mock(OrganizationEntity.class);
        when(existingOrganization.getId()).thenReturn("DEFAULT");
        when(organizationService.findByCockpitId(any())).thenReturn(existingOrganization);
        when(
            organizationService.createOrUpdate(
                argThat(orgaId -> orgaId.equals("DEFAULT")),
                argThat(
                    newOrganization ->
                        newOrganization.getCockpitId().equals(organizationPayload.cockpitId()) &&
                        newOrganization.getHrids().equals(organizationPayload.hrids()) &&
                        newOrganization.getDescription().equals(organizationPayload.description()) &&
                        newOrganization.getName().equals(organizationPayload.name())
                )
            )
        ).thenReturn(new OrganizationEntity());

        TestObserver<OrganizationReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handle_must_set_organization_context_during_processing() throws InterruptedException {
        OrganizationCommandPayload payload = OrganizationCommandPayload.builder()
            .id("orga#1")
            .cockpitId("org#cockpit-1")
            .hrids(Collections.singletonList("orga-1"))
            .description("Organization description")
            .name("Organization name")
            .build();

        OrganizationEntity existing = mock(OrganizationEntity.class);
        when(existing.getId()).thenReturn("ACME");
        when(organizationService.findByCockpitId("org#cockpit-1")).thenReturn(existing);

        AtomicReference<String> orgIdSeenInsideService = new AtomicReference<>();
        when(organizationService.createOrUpdate(eq("ACME"), any(UpdateOrganizationEntity.class))).thenAnswer(invocation -> {
            orgIdSeenInsideService.set(GraviteeContext.getCurrentOrganization());
            return new OrganizationEntity();
        });

        cut
            .handle(new OrganizationCommand(payload))
            .test()
            .await()
            .assertValue(reply -> reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        assertThat(orgIdSeenInsideService.get())
            .as("createOrUpdate must run with GraviteeContext bound to the resolved organization")
            .isEqualTo("ACME");
        assertThat(GraviteeContext.getCurrentOrganization())
            .as("GraviteeContext must be cleaned up after handle()")
            .isEqualTo(GraviteeContext.getDefaultOrganization());
    }

    @Test
    public void handle_must_clean_organization_context_when_service_throws() throws InterruptedException {
        OrganizationCommandPayload payload = OrganizationCommandPayload.builder()
            .id("orga#1")
            .cockpitId("org#cockpit-1")
            .hrids(Collections.singletonList("orga-1"))
            .description("Organization description")
            .name("Organization name")
            .build();

        OrganizationEntity existing = mock(OrganizationEntity.class);
        when(existing.getId()).thenReturn("ACME");
        when(organizationService.findByCockpitId("org#cockpit-1")).thenReturn(existing);
        when(organizationService.createOrUpdate(eq("ACME"), any(UpdateOrganizationEntity.class))).thenThrow(new RuntimeException("boom"));

        cut
            .handle(new OrganizationCommand(payload))
            .test()
            .await()
            .assertValue(reply -> reply.getCommandStatus().equals(CommandStatus.ERROR));

        assertThat(GraviteeContext.getCurrentOrganization())
            .as("GraviteeContext must be cleaned up even when the downstream service fails")
            .isEqualTo(GraviteeContext.getDefaultOrganization());
    }
}
