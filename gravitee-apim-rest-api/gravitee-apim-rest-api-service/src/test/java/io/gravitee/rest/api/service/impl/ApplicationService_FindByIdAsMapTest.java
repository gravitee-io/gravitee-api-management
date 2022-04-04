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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindByIdAsMapTest {

    private static final String APPLICATION_ID = "id-app";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Mock
    private EnvironmentService environmentService;

    @Test
    public void shouldFindByIdAsMap() throws TechnicalException {
        Application application = new Application();
        application.setEnvironmentId("env-id");
        application.setId("my-id");
        application.setName("my-name");
        application.setPicture("my-picture");
        application.setStatus(ApplicationStatus.ACTIVE);
        application.setDescription("my test application");

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("env-id");
        environment.setOrganizationId("org-id");

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(environmentService.findById("env-id")).thenReturn(environment);

        Map<String, Object> resultMap = applicationService.findByIdAsMap(APPLICATION_ID);

        assertEquals("my-id", resultMap.get("id"));
        assertEquals("my-name", resultMap.get("name"));
        assertEquals("ACTIVE", resultMap.get("status"));
        assertEquals("my test application", resultMap.get("description"));

        // picture has been explicitly removed from map data
        assertFalse(resultMap.containsKey("picture"));
    }
}
