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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CockpitIdInitializerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private CockpitIdInitializer initializer = new CockpitIdInitializer();

    @Test
    public void shouldSetCockpitIdOnOrgAndEnvs() {
        final EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId("env-1");
        env1.setOrganizationId("orgEntity-1");

        final EnvironmentEntity env2 = new EnvironmentEntity();
        env2.setOrganizationId("orgEntity-2");
        env2.setId("env-2");

        final OrganizationEntity orgEntity = new OrganizationEntity();
        orgEntity.setId("org-1");

        when(organizationService.findAll()).thenReturn(List.of(orgEntity));

        when(environmentService.findByOrganization("org-1")).thenReturn(List.of(env1, env2));

        initializer.initialize();

        verify(organizationService, times(1))
            .updateOrganization(any(ExecutionContext.class), argThat(org -> org.getCockpitId().equals("org-1")));

        verify(environmentService, times(1)).createOrUpdate(eq("org-1"), anyString(), argThat(env -> env.getCockpitId().equals("env-1")));

        verify(environmentService, times(1)).createOrUpdate(eq("org-1"), anyString(), argThat(env -> env.getCockpitId().equals("env-2")));
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(InitializerOrder.COCKPIT_ID_INITIALIZER, initializer.getOrder());
    }
}
