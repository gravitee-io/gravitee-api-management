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
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.EntrypointRepository;
import io.gravitee.repository.management.model.Entrypoint;
import io.gravitee.repository.management.model.EntrypointReferenceType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointRepositoryMock extends AbstractRepositoryMock<EntrypointRepository> {

    public EntrypointRepositoryMock() {
        super(EntrypointRepository.class);
    }

    @Override
    protected void prepare(EntrypointRepository entrypointRepository) throws Exception {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setId("new-entrypoint");
        entrypoint.setReferenceId("DEFAULT");
        entrypoint.setReferenceType(EntrypointReferenceType.ORGANIZATION);
        entrypoint.setValue("Entry point value");
        entrypoint.setTags("internal;product");

        final Entrypoint entrypoint2 = new Entrypoint();
        entrypoint2.setId("entrypoint");
        entrypoint2.setReferenceId("DEFAULT");
        entrypoint2.setReferenceType(EntrypointReferenceType.ORGANIZATION);
        entrypoint2.setValue("https://public-api.company.com");

        final Entrypoint entrypoint2Updated = new Entrypoint();
        entrypoint2Updated.setId("entrypoint");
        entrypoint2Updated.setReferenceId("DEFAULT");
        entrypoint2Updated.setReferenceType(EntrypointReferenceType.ORGANIZATION);
        entrypoint2Updated.setValue("New value");
        entrypoint2Updated.setTags("New tags");

        final Set<Entrypoint> entrypoints = newSet(entrypoint, entrypoint2, mock(Entrypoint.class));
        final Set<Entrypoint> entrypointsAfterDelete = newSet(entrypoint, entrypoint2);
        final Set<Entrypoint> entrypointsAfterAdd = newSet(entrypoint, entrypoint2, mock(Entrypoint.class), mock(Entrypoint.class));

        when(entrypointRepository.findByReference("DEFAULT", EntrypointReferenceType.ORGANIZATION))
            .thenReturn(entrypoints, entrypointsAfterAdd, entrypoints, entrypointsAfterDelete, entrypoints);

        when(entrypointRepository.create(any(Entrypoint.class))).thenReturn(entrypoint);

        when(entrypointRepository.findById("new-entrypoint")).thenReturn(of(entrypoint));
        when(entrypointRepository.findById("fa29c012-a0d2-4721-a9c0-12a0d26721db")).thenReturn(of(entrypoint2), of(entrypoint2Updated));

        when(entrypointRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
