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
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.NewOrganizationEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.impl.OrganizationServiceImpl;
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
public class OrganizationService_CreateTest {

    @InjectMocks
    private OrganizationServiceImpl organizationService = new OrganizationServiceImpl();

    @Mock
    private OrganizationRepository mockOrganizationRepository;

    @Mock
    private RoleService mockRoleService;

    @Test
    public void shouldCreateOrganization() throws TechnicalException {
        when(mockOrganizationRepository.findById(any())).thenReturn(Optional.empty());

        NewOrganizationEntity org1 = new NewOrganizationEntity();
        org1.setName("org_name");
        org1.setDescription("org_desc");
        List<String> domainRestrictions = Arrays.asList("domain", "restriction");
        org1.setDomainRestrictions(domainRestrictions);
        
        Organization createdOrganization = new Organization();
        createdOrganization.setId("created_org");
        when(mockOrganizationRepository.create(any())).thenReturn(createdOrganization);

        OrganizationEntity organization = organizationService.create(org1);

        assertNotNull("result is null", organization);
        verify(mockOrganizationRepository, times(1))
            .create(argThat(arg -> 
                arg != null 
                && arg.getName().equals("org_name")
                && arg.getDescription().equals("org_desc")
                && arg.getDomainRestrictions().equals(domainRestrictions)
            ));
        verify(mockRoleService, times(1)).initialize("created_org");
        verify(mockRoleService, times(1)).createOrUpdateSystemRoles("created_org");
    }
}
