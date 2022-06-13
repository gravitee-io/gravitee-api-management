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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.Optional;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class OrganizationService_GetDefaultOrInitializeTest {

    @InjectMocks
    private OrganizationServiceImpl organizationService = new OrganizationServiceImpl();

    @Mock
    private OrganizationRepository mockOrganizationRepository;

    @Mock
    private FlowService mockFlowService;

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnDefaultOrganization() throws TechnicalException {
        Organization existingDefault = new Organization();
        existingDefault.setId(GraviteeContext.getDefaultOrganization());
        when(mockOrganizationRepository.findById(eq(GraviteeContext.getDefaultOrganization()))).thenReturn(Optional.of(existingDefault));

        OrganizationEntity organization = organizationService.getDefaultOrInitialize();

        assertThat(organization.getId()).isEqualTo(GraviteeContext.getDefaultEnvironment());
    }

    @Test
    public void shouldCreateDefaultOrganization() throws TechnicalException {
        when(mockOrganizationRepository.findById(eq(GraviteeContext.getDefaultOrganization()))).thenReturn(Optional.empty());

        OrganizationEntity organization = organizationService.getDefaultOrInitialize();

        assertThat(organization.getId()).isEqualTo(GraviteeContext.getDefaultEnvironment());
        assertThat(organization.getName()).isEqualTo("Default organization");
        assertThat(organization.getHrids()).isEqualTo(Collections.singletonList("default"));
        assertThat(organization.getDescription()).isEqualTo("Default organization");
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldCatchExceptionIfThrow() throws TechnicalException {
        when(mockOrganizationRepository.findById(eq(GraviteeContext.getDefaultEnvironment()))).thenThrow(new TechnicalException(""));

        organizationService.getDefaultOrInitialize();
    }
}
