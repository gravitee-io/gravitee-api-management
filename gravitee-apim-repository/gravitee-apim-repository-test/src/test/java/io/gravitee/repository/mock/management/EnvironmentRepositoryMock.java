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

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.mockito.internal.util.collections.Sets;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentRepositoryMock extends AbstractRepositoryMock<EnvironmentRepository> {

    public EnvironmentRepositoryMock() {
        super(EnvironmentRepository.class);
    }

    @Override
    protected void prepare(EnvironmentRepository EnvironmentRepository) throws Exception {
        final Environment envCreate = new Environment();
        envCreate.setId("DEFAULT-create");
        envCreate.setCockpitId("cockpit-create");
        envCreate.setHrids(Arrays.asList("hrid1", "hrid2"));
        envCreate.setName("Default env for create");
        envCreate.setDescription("Default env description for create");
        envCreate.setOrganizationId("DEFAULT-ORG");
        envCreate.setDomainRestrictions(Arrays.asList("domain", "restriction"));

        final Environment env2Update = new Environment();
        env2Update.setId("DEFAULT-update");
        env2Update.setName("Default env for update");

        final Environment envUpdated = new Environment();
        envUpdated.setId("DEFAULT-update");
        envUpdated.setName("New name");
        envUpdated.setCockpitId("env#cockpit-new");

        final Environment envDelete = new Environment();
        envDelete.setId("DEFAULT-delete");
        envDelete.setName("Default env for delete");

        final Environment envFindById = new Environment();
        envFindById.setId("DEFAULT-findById");
        envFindById.setName("Default env for findById");
        envCreate.setOrganizationId("DEFAULT-ORG");

        when(EnvironmentRepository.create(any(Environment.class))).thenReturn(envCreate);
        when(EnvironmentRepository.update(any(Environment.class))).thenReturn(envUpdated);
        when(EnvironmentRepository.update(any(Environment.class))).thenReturn(envUpdated);

        when(EnvironmentRepository.findById("DEFAULT-create")).thenReturn(of(envCreate));
        when(EnvironmentRepository.findById("DEFAULT-update")).thenReturn(of(env2Update), of(envUpdated));
        when(EnvironmentRepository.findById("DEFAULT-delete")).thenReturn(of(envDelete), Optional.empty());
        when(EnvironmentRepository.findById("DEFAULT-findById")).thenReturn(of(envFindById));

        final Set<Environment> allEnvironments = newSet(envCreate, env2Update, envUpdated, envDelete, envFindById);
        final Set<Environment> orgEnvironments = newSet(envFindById);

        when(EnvironmentRepository.findAll()).thenReturn(allEnvironments);
        when(EnvironmentRepository.findByOrganization("DEFAULT-ORG")).thenReturn(orgEnvironments);
        when(EnvironmentRepository.findByOrganizationsAndHrids(Sets.newSet("DEFAULT-ORG", "ANOTHER-ORG"), Sets.newSet("def", "find")))
            .thenReturn(newSet(envCreate, envFindById));
        when(EnvironmentRepository.findByOrganizationsAndHrids(Sets.newSet(), Sets.newSet("def", "find")))
            .thenReturn(newSet(envCreate, envFindById));
        when(EnvironmentRepository.findByOrganizationsAndHrids(Sets.newSet("DEFAULT-ORG"), Sets.newSet())).thenReturn(newSet(envFindById));
        when(EnvironmentRepository.findByOrganizationsAndHrids(Sets.newSet(), Sets.newSet())).thenReturn(newSet());
        when(EnvironmentRepository.findByCockpit("cockpitId-findById")).thenReturn(Optional.of(envFindById));
    }
}
