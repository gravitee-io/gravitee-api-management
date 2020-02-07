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
package io.gravitee.rest.api.services.v3.upgrader;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class V3UpgraderServiceTest {

    @InjectMocks
    V3UpgraderService service = new V3UpgraderService();

    @Mock
    IdentityProviderRepository identityProviderRepository;

    @Mock
    RoleRepository roleRepository;

    @Test
    public void shouldUpdateIdentityProvidersWithOrganizationRole() throws TechnicalException {
        String[] roles = { "1:ADMIN", "2:USER" };
        Map<String, String[]> roleMappings = new HashMap<>();
        roleMappings.put("KEY", roles);
        IdentityProvider idp = new IdentityProvider();
        idp.setId("my-idp");
        idp.setRoleMappings(roleMappings);
        idp.setReferenceId("DEFAULT");
        idp.setReferenceType(IdentityProviderReferenceType.ORGANIZATION);
        
        when(identityProviderRepository.findAll()).thenReturn(Collections.singleton(idp));
        when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.ORGANIZATION, "ADMIN", "DEFAULT", RoleReferenceType.ORGANIZATION)).thenReturn(Optional.of(new Role()));
        
        service.convertIDPRoleMapping();

        ArgumentCaptor<IdentityProvider> idpCaptor = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(roleRepository, times(1)).findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any());
        verify(identityProviderRepository).update(idpCaptor.capture());
        IdentityProvider updatedIdp = idpCaptor.getValue();
        String[] newRoles = updatedIdp.getRoleMappings().get("KEY");
        assertEquals(3, newRoles.length);
        assertTrue(newRoles[0].equals("ORGANIZATION:ADMIN") || newRoles[0].equals("ENVIRONMENT:ADMIN") || newRoles[0].equals("ENVIRONMENT:USER"));
        assertTrue(newRoles[1].equals("ORGANIZATION:ADMIN") || newRoles[1].equals("ENVIRONMENT:ADMIN") || newRoles[1].equals("ENVIRONMENT:USER"));
        assertTrue(newRoles[2].equals("ORGANIZATION:ADMIN") || newRoles[2].equals("ENVIRONMENT:ADMIN") || newRoles[2].equals("ENVIRONMENT:USER"));
    }
    
    @Test
    public void shouldUpdateIdentityProvidersWithoutOrganizationRole() throws TechnicalException {
        String[] roles = { "1:ADMIN", "2:USER" };
        Map<String, String[]> roleMappings = new HashMap<>();
        roleMappings.put("KEY", roles);
        IdentityProvider idp = new IdentityProvider();
        idp.setId("my-idp");
        idp.setRoleMappings(roleMappings);
        idp.setReferenceId("DEFAULT");
        idp.setReferenceType(IdentityProviderReferenceType.ORGANIZATION);
        
        when(identityProviderRepository.findAll()).thenReturn(Collections.singleton(idp));
        when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.ORGANIZATION, "ADMIN", "DEFAULT", RoleReferenceType.ORGANIZATION)).thenReturn(Optional.empty());
        
        service.convertIDPRoleMapping();

        ArgumentCaptor<IdentityProvider> idpCaptor = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(roleRepository, times(1)).findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any());
        verify(identityProviderRepository).update(idpCaptor.capture());
        IdentityProvider updatedIdp = idpCaptor.getValue();
        String[] newRoles = updatedIdp.getRoleMappings().get("KEY");
        assertEquals(2, newRoles.length);
        assertTrue(newRoles[0].equals("ENVIRONMENT:ADMIN") || newRoles[0].equals("ENVIRONMENT:USER"));
        assertTrue(newRoles[1].equals("ENVIRONMENT:ADMIN") || newRoles[1].equals("ENVIRONMENT:USER"));
    }
    
    @Test
    public void shouldDoNothing() throws TechnicalException {
        String[] roles = { "ORGANIZATION:ADMIN", "ENVIRONMENT:USER" };
        Map<String, String[]> roleMappings = new HashMap<>();
        roleMappings.put("KEY", roles);
        IdentityProvider idp = new IdentityProvider();
        idp.setId("my-idp");
        idp.setRoleMappings(roleMappings);
        idp.setReferenceId("DEFAULT");
        idp.setReferenceType(IdentityProviderReferenceType.ORGANIZATION);
        
        when(identityProviderRepository.findAll()).thenReturn(Collections.singleton(idp));
        
        service.convertIDPRoleMapping();

        verify(roleRepository, never()).findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any());
        verify(identityProviderRepository, never()).update(any());
    }
    
    @Test
    public void shouldDoNothingWithNoRoleMapping() throws TechnicalException {
        IdentityProvider idp = new IdentityProvider();
        idp.setId("my-idp");
        idp.setReferenceId("DEFAULT");
        idp.setReferenceType(IdentityProviderReferenceType.ORGANIZATION);
        
        when(identityProviderRepository.findAll()).thenReturn(Collections.singleton(idp));
        
        service.convertIDPRoleMapping();

        verify(roleRepository, never()).findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any());
        verify(identityProviderRepository, never()).update(any());
    }
}
