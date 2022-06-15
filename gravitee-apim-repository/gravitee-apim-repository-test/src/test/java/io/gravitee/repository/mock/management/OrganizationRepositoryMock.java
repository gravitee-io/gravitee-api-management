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

import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OrganizationRepositoryMock extends AbstractRepositoryMock<OrganizationRepository> {

    public OrganizationRepositoryMock() {
        super(OrganizationRepository.class);
    }

    @Override
    protected void prepare(OrganizationRepository organizationRepository) throws Exception {
        final Organization orgCreate = new Organization();
        orgCreate.setId("DEFAULT-ORG-create");
        orgCreate.setCockpitId("cockpit-org-create");
        orgCreate.setHrids(Arrays.asList("hrid1", "hrid2"));
        orgCreate.setName("Default org for create");
        orgCreate.setDescription("Default org description for create");
        orgCreate.setDomainRestrictions(Arrays.asList("domain", "restriction"));
        orgCreate.setFlowMode("BEST_MATCH");

        final Organization org2Update = new Organization();
        org2Update.setId("DEFAULT-ORG-update");
        org2Update.setName("Default org for update");

        final Organization orgUpdated = new Organization();
        orgUpdated.setId("DEFAULT-ORG-update");
        orgUpdated.setName("New name");
        orgUpdated.setCockpitId("org#cockpit-new");
        orgUpdated.setDescription("New description");
        orgUpdated.setDomainRestrictions(Collections.singletonList("New domain restriction"));
        orgUpdated.setHrids(Collections.singletonList("New hrid"));
        orgUpdated.setFlowMode("DEFAULT");

        final Organization orgDelete = new Organization();
        orgDelete.setId("DEFAULT-ORG-delete");
        orgDelete.setName("Default org for delete");

        final Organization orgFindById = new Organization();
        orgFindById.setId("DEFAULT-ORG-findById");
        orgFindById.setName("Default org for findById");

        final Organization orgDefault = new Organization();
        orgDefault.setId("DEFAULT");
        orgDefault.setName("Default org");

        when(organizationRepository.create(any(Organization.class))).thenReturn(orgCreate);
        when(organizationRepository.update(any(Organization.class))).thenReturn(orgUpdated);
        when(organizationRepository.update(any(Organization.class))).thenReturn(orgUpdated);

        when(organizationRepository.findById("DEFAULT-ORG-create")).thenReturn(of(orgCreate));
        when(organizationRepository.findById("DEFAULT-ORG-update")).thenReturn(of(org2Update), of(orgUpdated));
        when(organizationRepository.findById("DEFAULT-ORG-delete")).thenReturn(of(orgDelete), Optional.empty());
        when(organizationRepository.findById("DEFAULT-ORG-findById")).thenReturn(of(orgFindById));

        when(organizationRepository.count()).thenReturn(4L);
        when(organizationRepository.findAll()).thenReturn(Set.of(orgCreate, orgUpdated, orgFindById, orgDefault));

        when(organizationRepository.findByHrids(newSet("def", "ate"))).thenReturn(newSet(orgFindById, orgUpdated));
    }
}
