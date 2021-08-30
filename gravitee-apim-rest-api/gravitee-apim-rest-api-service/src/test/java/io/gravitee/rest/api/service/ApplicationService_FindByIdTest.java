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
package io.gravitee.rest.api.service;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindByIdTest {

    private static final String APPLICATION_ID = "id-app";
    private static final String ENV_ID = "DEFAULT";
    private static final String ORG_ID = "DEFAULT";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Mock
    private Application application;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentEntity environmentEntity;

    @Before
    public void setUp() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        when(application.getEnvironmentId()).thenReturn(ENV_ID);
        when(environmentEntity.getOrganizationId()).thenReturn(ORG_ID);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);

        final ApplicationEntity applicationEntity = applicationService.findById(APPLICATION_ID);

        assertNotNull(applicationEntity);
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotFindByIdBecauseNotExists() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        applicationService.findById(APPLICATION_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenThrow(TechnicalException.class);

        applicationService.findById(APPLICATION_ID);
    }
}
