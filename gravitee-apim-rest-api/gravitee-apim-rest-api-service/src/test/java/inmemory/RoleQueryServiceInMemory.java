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
package inmemory;

import fixtures.core.model.RoleFixtures;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class can be reset with all system roles for an organization.
 * <p>
 * ⚠️ Currently, the role permissions are not initialized in the roles.
 * </p>
 */
public class RoleQueryServiceInMemory implements RoleQueryService, InMemoryAlternative<Role> {

    private final List<Role> storage = new ArrayList<>();

    @Override
    public Optional<Role> findApiRole(String name, ReferenceContext referenceContext) {
        return storage
            .stream()
            .filter(role -> role.getScope().equals(Role.Scope.API))
            .filter(role -> role.getReferenceType().name().equals(referenceContext.getReferenceType().name()))
            .filter(role -> role.getReferenceId().equals(referenceContext.getReferenceId()))
            .filter(role -> role.getName().equals(name))
            .findFirst();
    }

    @Override
    public Optional<Role> findApplicationRole(String name, ReferenceContext referenceContext) {
        return storage
            .stream()
            .filter(role -> role.getScope().equals(Role.Scope.APPLICATION))
            .filter(role -> role.getReferenceType().name().equals(referenceContext.getReferenceType().name()))
            .filter(role -> role.getReferenceId().equals(referenceContext.getReferenceId()))
            .filter(role -> role.getName().equals(name))
            .findFirst();
    }

    @Override
    public Optional<Role> findIntegrationRole(String name, ReferenceContext referenceContext) {
        return storage
            .stream()
            .filter(role -> role.getScope().equals(Role.Scope.INTEGRATION))
            .filter(role -> role.getReferenceType().name().equals(referenceContext.getReferenceType().name()))
            .filter(role -> role.getReferenceId().equals(referenceContext.getReferenceId()))
            .filter(role -> role.getName().equals(name))
            .findFirst();
    }

    @Override
    public Set<Role> findByIds(Set<String> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Set.of();
        }

        return storage.stream().filter(role -> ids.contains(role.getId())).collect(Collectors.toSet());
    }

    public void resetSystemRoles(String organizationId) {
        this.storage.clear();
        // Organization Admin
        this.storage.add(RoleFixtures.anOrganizationAdminRole(organizationId));
        // Environment Admin
        this.storage.add(RoleFixtures.anEnvironmentAdminRole(organizationId));
        // API Primary Owner
        this.storage.add(RoleFixtures.anApiPrimaryOwnerRole(organizationId));
        // Application Primary Owner
        this.storage.add(RoleFixtures.anApplicationPrimaryOwnerRole(organizationId));
        // Group Admin
        this.storage.add(RoleFixtures.aGroupAdminRole(organizationId));
        //Integration Primary Owner
        this.storage.add(RoleFixtures.anIntegrationPrimaryOwnerRole(organizationId));
    }

    @Override
    public void initWith(List<Role> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Role> storage() {
        return Collections.unmodifiableList(storage);
    }
}
