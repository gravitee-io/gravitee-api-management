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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.NewEnvironmentEntity;
import io.gravitee.rest.api.service.impl.EnvironmentServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentService_CreateTest {

    @InjectMocks
    private EnvironmentServiceImpl environmentService = new EnvironmentServiceImpl();

    @Mock
    private EnvironmentRepository mockEnvironmentRepository;

    @Mock
    private OrganizationService mockOrganizationService;

    @Mock
    private ApiHeaderService mockAPIHeaderService;
    
    @Mock
    private ViewService mockViewService;
    
    @Mock
    private PageService mockPageService;
    
    @Test
    public void shouldCreateEnvironment() throws TechnicalException {
        when(mockOrganizationService.findById(any())).thenReturn(null);
        when(mockEnvironmentRepository.findById(any())).thenReturn(Optional.empty());

        NewEnvironmentEntity env1 = new NewEnvironmentEntity();
        env1.setName("env_name");
        env1.setDescription("env_desc");
        List<String> domainRestrictions = Arrays.asList("domain", "restriction");
        env1.setDomainRestrictions(domainRestrictions);
        
        Environment createdEnv = new Environment();
        createdEnv.setId("created_env");
        when(mockEnvironmentRepository.create(any())).thenReturn(createdEnv);

        EnvironmentEntity environment = environmentService.create(env1);

        assertNotNull("result is null", environment);
        verify(mockEnvironmentRepository, times(1))
            .create(argThat(arg -> 
                arg != null 
                && arg.getName().equals("env_name")
                && arg.getDescription().equals("env_desc")
                && arg.getDomainRestrictions().equals(domainRestrictions)
                && arg.getOrganization().equals("DEFAULT")
            ));
        verify(mockAPIHeaderService, times(1)).initialize("created_env");
        verify(mockViewService, times(1)).initialize("created_env");
        verify(mockPageService, times(1)).initialize("created_env");
    }
}
