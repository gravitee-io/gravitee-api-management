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

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteOrganizationCommandHandlerTest {

    @Mock
    private AccessPointRepository accessPointRepository;

    @Mock
    private FlowRepository flowRepository;

    @Mock
    private ParameterRepository parameterRepository;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private AccessPointCrudService accessPointService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    private DeleteOrganizationCommandHandler cut;

    @Before
    public void setUp() throws Exception {
        cut =
            new DeleteOrganizationCommandHandler(
                accessPointRepository,
                flowRepository,
                parameterRepository,
                auditRepository,
                organizationService,
                accessPointService,
                identityProviderActivationService
            );
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.DELETE_ORGANIZATION.name(), cut.supportType());
    }
}
