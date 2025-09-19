/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RoleService_FindByIdAndOrganizationId {

    private static final String ROLE_ID = "my-role";
    private static final String ORGANIZATION_ID = "DEFAULT";

    @InjectMocks
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @Mock
    private RoleRepository roleRepository;

    @BeforeEach
    public void setUp() {
        GraviteeContext.getCurrentRoles().clear();
    }

    @Test
    public void should_not_find_role() throws TechnicalException {
        when(roleRepository.findByIdAndReferenceIdAndReferenceType(ROLE_ID, ORGANIZATION_ID, RoleReferenceType.ORGANIZATION)).thenReturn(
            Optional.empty()
        );

        assertThat(roleService.findByIdAndOrganizationId(ROLE_ID, ORGANIZATION_ID)).isEmpty();
    }

    @Test
    public void should_find_role() throws TechnicalException {
        var role = new Role();
        role.setId(ROLE_ID);

        when(roleRepository.findByIdAndReferenceIdAndReferenceType(ROLE_ID, ORGANIZATION_ID, RoleReferenceType.ORGANIZATION)).thenReturn(
            Optional.of(role)
        );
        assertThat(roleService.findByIdAndOrganizationId(ROLE_ID, ORGANIZATION_ID)).isNotNull();
    }

    @Test
    public void should_throw_exception() throws TechnicalException {
        when(roleRepository.findByIdAndReferenceIdAndReferenceType(ROLE_ID, ORGANIZATION_ID, RoleReferenceType.ORGANIZATION)).thenThrow(
            new TechnicalException()
        );

        assertThrows(TechnicalManagementException.class, () -> roleService.findByIdAndOrganizationId(ROLE_ID, ORGANIZATION_ID));
    }
}
