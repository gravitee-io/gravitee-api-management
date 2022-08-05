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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindByOrganizationTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private final ApplicationService applicationService = new ApplicationServiceImpl();

    @Test
    public void shouldReturnEmptyListWithNullOrganization() {
        assertThat(applicationService.findByOrganization(null)).isEmpty();
    }

    @Test
    public void shouldReturnEmptyListWithEmptyOrganization() {
        assertThat(applicationService.findByOrganization("")).isEmpty();
    }

    @Test
    public void shouldFindByOrganization() throws TechnicalException {
        final String organizationId = "gravitee";
        final String environmentId = "test";
        final EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(environmentId);
        environment.setOrganizationId(organizationId);

        final Application application = new Application();
        application.setId("test-application");
        application.setType(ApplicationType.WEB);
        application.setStatus(ApplicationStatus.ACTIVE);
        when(environmentService.findByOrganization(organizationId)).thenReturn(List.of(environment));
        ApplicationCriteria criteria = new ApplicationCriteria.Builder().environmentIds(environmentId).build();
        when(applicationRepository.search(criteria, null)).thenReturn(new Page<>(List.of(application), 0, 1, 1));

        Set<ApplicationListItem> applications = applicationService.findByOrganization(organizationId);

        assertThat(applications).hasSize(1);
        assertThat(applications).extracting(ApplicationListItem::getId).containsExactly("test-application");
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementException() throws TechnicalException {
        final String organizationId = "gravitee";
        final String environmentId = "test";
        final EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(environmentId);
        environment.setOrganizationId(organizationId);
        when(environmentService.findByOrganization(organizationId)).thenReturn(List.of(environment));
        when(applicationRepository.search(any(), any())).thenThrow(new TechnicalException());
        applicationService.findByOrganization(organizationId);
    }
}
