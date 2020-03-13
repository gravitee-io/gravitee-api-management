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
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.service.exceptions.BadOrganizationException;
import io.gravitee.rest.api.service.impl.EnvironmentServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
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

        UpdateEnvironmentEntity env1 = new UpdateEnvironmentEntity();
        env1.setId("env_id");
        env1.setName("env_name");
        env1.setDescription("env_desc");
        List<String> domainRestrictions = Arrays.asList("domain", "restriction");
        env1.setDomainRestrictions(domainRestrictions);
        env1.setOrganizationId("DEFAULT");
        
        Environment createdEnv = new Environment();
        createdEnv.setId("env_id");
        when(mockEnvironmentRepository.create(any())).thenReturn(createdEnv);

        EnvironmentEntity environment = environmentService.createOrUpdate(env1);

        assertNotNull("result is null", environment);
        verify(mockEnvironmentRepository, times(1))
            .create(argThat(arg -> 
                arg != null 
                && arg.getName().equals("env_name")
                && arg.getDescription().equals("env_desc")
                && arg.getDomainRestrictions().equals(domainRestrictions)
                && arg.getOrganization().equals("DEFAULT")
            ));
        verify(mockEnvironmentRepository, never()).update(any());
        verify(mockAPIHeaderService, times(1)).initialize("env_id");
        verify(mockViewService, times(1)).initialize("env_id");
        verify(mockPageService, times(1)).initialize("env_id");
    }
    
    @Test
    public void shouldUpdateEnvironment() throws TechnicalException {
        when(mockOrganizationService.findById(any())).thenReturn(null);
        when(mockEnvironmentRepository.findById(any())).thenReturn(Optional.of(new Environment()));

        UpdateEnvironmentEntity env1 = new UpdateEnvironmentEntity();
        env1.setId("env_id");
        env1.setName("env_name");
        env1.setDescription("env_desc");
        List<String> domainRestrictions = Arrays.asList("domain", "restriction");
        env1.setDomainRestrictions(domainRestrictions);
        env1.setOrganizationId("DEFAULT");

        Environment updatedEnv = new Environment();
        when(mockEnvironmentRepository.update(any())).thenReturn(updatedEnv);

        EnvironmentEntity environment = environmentService.createOrUpdate(env1);

        assertNotNull("result is null", environment);
        verify(mockEnvironmentRepository, times(1))
            .update(argThat(arg -> 
                arg != null 
                && arg.getName().equals("env_name")
                && arg.getDescription().equals("env_desc")
                && arg.getDomainRestrictions().equals(domainRestrictions)
                && arg.getOrganization().equals("DEFAULT")
            ));
        verify(mockEnvironmentRepository, never()).create(any());
        verify(mockAPIHeaderService, never()).initialize("env_id");
        verify(mockViewService, never()).initialize("env_id");
        verify(mockPageService, never()).initialize("env_id");
    }
    
    @Test(expected = BadOrganizationException.class)
    public void shouldHaveBadOrganizationException() throws TechnicalException {
        UpdateEnvironmentEntity env1 = new UpdateEnvironmentEntity();
        env1.setOrganizationId("NOT DEFAULT");

        environmentService.createOrUpdate(env1);
    }
    
    @Test(expected = BadOrganizationException.class)
    public void shouldHaveBadOrganizationExceptionWhenNoOrganizationInEntity() throws TechnicalException {
        environmentService.createOrUpdate(new UpdateEnvironmentEntity());
    }
}
