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
package io.gravitee.rest.api.service.impl.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationUpgraderTest {

    @InjectMocks
    @Spy
    private OrganizationUpgrader upgrader = new OrganizationUpgrader() {
        protected void upgradeOrganization(ExecutionContext executionContext) {}

        public int getOrder() {
            return 0;
        }
    };

    @Mock
    private OrganizationRepository organizationRepository;

    @Test
    public void upgrade_should_read_all_organizations() throws Exception {
        upgrader.upgrade();

        verify(organizationRepository, times(1)).findAll();
    }

    @Test
    public void upgrade_should_call_upgradeOrganization_for_each_organization() throws Exception {
        when(organizationRepository.findAll())
            .thenReturn(Set.of(buildTestOrganization("org1"), buildTestOrganization("org2"), buildTestOrganization("org3")));

        upgrader.upgrade();

        verify(upgrader, times(1)).upgradeOrganization(argThat(e -> !e.hasEnvironmentId() && e.getOrganizationId().equals("org1")));
        verify(upgrader, times(1)).upgradeOrganization(argThat(e -> !e.hasEnvironmentId() && e.getOrganizationId().equals("org2")));
        verify(upgrader, times(1)).upgradeOrganization(argThat(e -> !e.hasEnvironmentId() && e.getOrganizationId().equals("org1")));
        verify(upgrader, times(1)).upgradeOrganization(argThat(e -> !e.hasEnvironmentId() && e.getOrganizationId().equals("org3")));
    }

    @Test
    public void upgrade_should_return_true_when_no_technicalException() throws Exception {
        boolean result = upgrader.upgrade();

        assertTrue(result);
    }

    @Test
    public void upgrade_should_return_false_when_technicalException() throws Exception {
        when(organizationRepository.findAll()).thenThrow(new TechnicalException("this is a test exception"));

        boolean result = upgrader.upgrade();

        assertFalse(result);
    }

    private Organization buildTestOrganization(String organizationId) {
        Organization organization = new Organization();
        organization.setId(organizationId);
        return organization;
    }
}
