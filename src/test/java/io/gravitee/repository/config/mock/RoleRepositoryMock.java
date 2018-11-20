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
package io.gravitee.repository.config.mock;

import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.management.model.View;
import org.mockito.ArgumentMatcher;

import java.util.Date;
import java.util.Set;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoleRepositoryMock extends AbstractRepositoryMock<RoleRepository> {

    public RoleRepositoryMock() {
        super(RoleRepository.class);
    }

    @Override
    void prepare(RoleRepository roleRepository) throws Exception {
        final Role toCreate = mock(Role.class);
        when(toCreate.getName()).thenReturn("to create");
        when(toCreate.getScope()).thenReturn(RoleScope.API);
        when(toCreate.getPermissions()).thenReturn(new int[]{3});

        final Role toDelete = mock(Role.class);
        when(toDelete.getName()).thenReturn("to delete");
        when(toDelete.getScope()).thenReturn(RoleScope.MANAGEMENT);
        when(toDelete.getPermissions()).thenReturn(new int[]{1, 2, 3});

        final Role toUpdate = mock(Role.class);
        when(toUpdate.getName()).thenReturn("to update");
        when(toUpdate.getDescription()).thenReturn("new description");
        when(toUpdate.getScope()).thenReturn(RoleScope.MANAGEMENT);
        when(toUpdate.isDefaultRole()).thenReturn(true);
        when(toUpdate.getPermissions()).thenReturn(new int[]{4, 5});

        final Role findByScope1 = mock(Role.class);
        when(findByScope1.getName()).thenReturn("find by scope 1");
        when(findByScope1.getDescription()).thenReturn("role description");
        when(findByScope1.getScope()).thenReturn(RoleScope.PORTAL);
        when(findByScope1.isDefaultRole()).thenReturn(true);
        when(findByScope1.isSystem()).thenReturn(true);
        when(findByScope1.getPermissions()).thenReturn(new int[]{1});

        final Role findByScope2 = mock(Role.class);
        when(findByScope2.getName()).thenReturn("find by scope 2");
        when(findByScope2.getScope()).thenReturn(RoleScope.PORTAL);
        when(findByScope2.isDefaultRole()).thenReturn(false);
        when(findByScope2.getPermissions()).thenReturn(new int[]{1});

        when(roleRepository.findById(findByScope1.getScope(), findByScope1.getName())).thenReturn(of(findByScope1));
        when(roleRepository.findById(toUpdate.getScope(), toUpdate.getName())).thenReturn(of(toUpdate));
        when(roleRepository.findById(toCreate.getScope(), toCreate.getName())).thenReturn(empty(), of(findByScope1));
        when(roleRepository.findById(toDelete.getScope(), toDelete.getName())).thenReturn(of(findByScope1), empty());
        when(roleRepository.findById(findByScope2.getScope(), findByScope2.getName())).thenReturn(of(findByScope2));
        when(roleRepository.create(any(Role.class))).thenReturn(toCreate);
        when(roleRepository.findAll()).thenReturn(newSet(toDelete, toUpdate, findByScope1, findByScope2));
        when(roleRepository.findByScope(RoleScope.PORTAL)).thenReturn(newSet(findByScope1, findByScope2));
        when(roleRepository.update(any())).thenReturn(toUpdate);

        when(roleRepository.update(argThat(new ArgumentMatcher<Role>() {
            @Override
            public boolean matches(Object o) {
                return o == null || (o instanceof Role && ((Role) o).getName().equals("unknown"));
            }
        }))).thenThrow(new IllegalStateException());
    }
}
