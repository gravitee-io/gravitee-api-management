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
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
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
public class ApplicationService_FindByStatusTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private final ApplicationService applicationService = new ApplicationServiceImpl();

    @Test
    public void shouldReturnEmptyListIfNoApplications() throws TechnicalException {
        final String organizationId = "gravitee";
        final String environmentId = "test";
        final String status = "ARCHIVED";
        final ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);

        when(applicationRepository.findAllByEnvironment(environmentId, ApplicationStatus.ARCHIVED)).thenReturn(Set.of());

        assertThat(applicationService.findByStatus(executionContext, status)).isEmpty();
    }

    @Test
    public void shouldReturnSimpleListIfStatusIsArchived() throws TechnicalException {
        final String organizationId = "gravitee";
        final String environmentId = "test";
        final String status = "ARCHIVED";
        final ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);

        Application application = new Application();
        application.setId("test-application");
        application.setType(ApplicationType.WEB);
        application.setStatus(ApplicationStatus.ARCHIVED);

        when(applicationRepository.findAllByEnvironment(environmentId, ApplicationStatus.ARCHIVED)).thenReturn(Set.of(application));

        Set<ApplicationListItem> applications = applicationService.findByStatus(executionContext, status);
        assertThat(applications).hasSize(1).extracting(ApplicationListItem::getStatus).containsExactly("ARCHIVED");
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementException() throws TechnicalException {
        final String organizationId = "gravitee";
        final String environmentId = "test";
        final String status = "ARCHIVED";
        final ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);
        when(applicationRepository.findAllByEnvironment(environmentId, ApplicationStatus.ARCHIVED)).thenThrow(new TechnicalException());
        applicationService.findByStatus(executionContext, status);
    }
}
