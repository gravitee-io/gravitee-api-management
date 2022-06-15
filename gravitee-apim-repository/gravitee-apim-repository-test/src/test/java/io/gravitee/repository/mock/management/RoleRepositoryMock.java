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
package io.gravitee.repository.mock.management;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoleRepositoryMock extends AbstractRepositoryMock<RoleRepository> {

    private static final RoleReferenceType REFERENCE_TYPE = RoleReferenceType.ORGANIZATION;
    private static final String REFERENCE_ID = "DEFAULT";

    public RoleRepositoryMock() {
        super(RoleRepository.class);
    }

    @Override
    protected void prepare(RoleRepository roleRepository) throws Exception {
        final Role toCreate = mock(Role.class);
        when(toCreate.getId()).thenReturn("API_to_create");
        when(toCreate.getName()).thenReturn("to create");
        when(toCreate.getReferenceId()).thenReturn(REFERENCE_ID);
        when(toCreate.getReferenceType()).thenReturn(REFERENCE_TYPE);
        when(toCreate.getScope()).thenReturn(RoleScope.API);
        when(toCreate.getPermissions()).thenReturn(new int[] { 3 });

        final Role toDelete = mock(Role.class);
        when(toDelete.getId()).thenReturn("ORGANIZATION_to_delete");
        when(toDelete.getName()).thenReturn("to delete");
        when(toDelete.getReferenceId()).thenReturn(REFERENCE_ID);
        when(toDelete.getReferenceType()).thenReturn(REFERENCE_TYPE);
        when(toDelete.getScope()).thenReturn(RoleScope.ORGANIZATION);
        when(toDelete.getPermissions()).thenReturn(new int[] { 1, 2, 3 });

        final Role toUpdate = mock(Role.class);
        when(toUpdate.getId()).thenReturn("ENVIRONMENT_to_update");
        when(toUpdate.getName()).thenReturn("to update");
        when(toUpdate.getReferenceId()).thenReturn(REFERENCE_ID);
        when(toUpdate.getReferenceType()).thenReturn(REFERENCE_TYPE);
        when(toUpdate.getDescription()).thenReturn("new description");
        when(toUpdate.getScope()).thenReturn(RoleScope.ENVIRONMENT);
        when(toUpdate.isDefaultRole()).thenReturn(true);
        when(toUpdate.getPermissions()).thenReturn(new int[] { 4, 5 });

        final Role findByScope1 = mock(Role.class);
        when(findByScope1.getId()).thenReturn("API_find_by_scope_1");
        when(findByScope1.getName()).thenReturn("find by scope 1");
        when(findByScope1.getReferenceId()).thenReturn(REFERENCE_ID);
        when(findByScope1.getReferenceType()).thenReturn(REFERENCE_TYPE);
        when(findByScope1.getDescription()).thenReturn("role description");
        when(findByScope1.getScope()).thenReturn(RoleScope.API);
        when(findByScope1.isDefaultRole()).thenReturn(true);
        when(findByScope1.isSystem()).thenReturn(true);
        when(findByScope1.getPermissions()).thenReturn(new int[] { 1 });

        final Role findByScope2 = mock(Role.class);
        when(findByScope2.getId()).thenReturn("API_find_by_scope_2");
        when(findByScope2.getName()).thenReturn("find by scope 2");
        when(findByScope2.getReferenceId()).thenReturn(REFERENCE_ID);
        when(findByScope2.getReferenceType()).thenReturn(REFERENCE_TYPE);
        when(findByScope2.getScope()).thenReturn(RoleScope.API);
        when(findByScope2.isDefaultRole()).thenReturn(false);
        when(findByScope2.getPermissions()).thenReturn(new int[] { 1 });

        when(roleRepository.findById("API_find_by_scope_1")).thenReturn(of(findByScope1));
        when(roleRepository.findById("ENVIRONMENT_to_update")).thenReturn(of(toUpdate));
        when(roleRepository.findById("API_to_create")).thenReturn(empty(), of(toCreate));
        when(roleRepository.findById("ORGANIZATION_to_delete")).thenReturn(of(findByScope1), empty());
        when(roleRepository.findById("API_find_by_scope_2")).thenReturn(of(findByScope2));
        when(roleRepository.create(any(Role.class))).thenReturn(toCreate);
        when(roleRepository.findAll()).thenReturn(newSet(toDelete, toUpdate, findByScope1, findByScope2));
        when(roleRepository.findAllByReferenceIdAndReferenceType(REFERENCE_ID, REFERENCE_TYPE))
            .thenReturn(newSet(toCreate, toDelete, toUpdate));

        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.API, "find by scope 1", REFERENCE_ID, REFERENCE_TYPE)
        )
            .thenReturn(Optional.of(findByScope1));
        when(roleRepository.findByScopeAndReferenceIdAndReferenceType(RoleScope.API, REFERENCE_ID, REFERENCE_TYPE))
            .thenReturn(newSet(findByScope1));

        when(roleRepository.update(any())).thenReturn(toUpdate);

        when(roleRepository.update(argThat(o -> o == null || o.getName().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
