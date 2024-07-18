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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class RoleService_FindAllById {

    private static final String ROLE_ID = "role#1";

    @InjectMocks
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @Mock
    private RoleRepository mockRoleRepository;

    @BeforeEach
    public void init() {
        GraviteeContext.getCurrentRoles().clear();
    }

    @Test
    @SneakyThrows
    public void should_throw_exception_when_an_error_occurs() {
        when(mockRoleRepository.findAllById(Set.of(ROLE_ID))).thenThrow(TechnicalException.class);

        assertThatThrownBy(() -> roleService.findAllById(Set.of(ROLE_ID))).isInstanceOf(TechnicalManagementException.class);
    }

    @Test
    @SneakyThrows
    public void should_return_roles() {
        var role = new Role();
        role.setId(ROLE_ID);
        when(mockRoleRepository.findAllById(Set.of(ROLE_ID))).thenReturn(Set.of(role));

        assertThat(roleService.findAllById(Set.of(ROLE_ID))).hasSize(1).extracting(RoleEntity::getId).containsExactly(ROLE_ID);
    }
}
