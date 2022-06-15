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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.model.Installation;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InstallationRepositoryMock extends AbstractRepositoryMock<InstallationRepository> {

    public InstallationRepositoryMock() {
        super(InstallationRepository.class);
    }

    @Override
    protected void prepare(InstallationRepository installationRepositoryRepository) throws Exception {
        final Installation createInstallation = new Installation();
        createInstallation.setId("1");
        createInstallation.setCreatedAt(new Date(1000000000000L));
        createInstallation.setUpdatedAt(new Date(1100000000000L));
        Map<String, String> additionalInformation = new HashMap<>();
        additionalInformation.put("key1", "value1");
        additionalInformation.put("key2", "value2");
        createInstallation.setAdditionalInformation(additionalInformation);
        when(installationRepositoryRepository.create(any())).thenReturn(createInstallation);

        final Installation installation_found = new Installation();
        installation_found.setId("installation-1");
        installation_found.setCreatedAt(new Date(1000000000000L));
        installation_found.setUpdatedAt(new Date(1100000000000L));
        Map<String, String> additionalInformation2 = new HashMap<>();
        additionalInformation2.put("key1", "value1");
        additionalInformation2.put("key2", "value2");
        installation_found.setAdditionalInformation(additionalInformation2);

        final Installation installation_delete = new Installation();
        installation_delete.setId("installation-to-delete");

        final Installation installation_updated = new Installation();
        installation_updated.setId("group-application-1");
        installation_updated.setId("installation-1");
        installation_updated.setCreatedAt(new Date(1000000000000L));
        installation_updated.setUpdatedAt(new Date(1200000000000L));
        Map<String, String> additionalInformation3 = new HashMap<>();
        additionalInformation3.put("key1", "value1");
        additionalInformation3.put("key2", "value22");
        additionalInformation3.put("key3", "value3");
        installation_updated.setAdditionalInformation(additionalInformation3);

        when(installationRepositoryRepository.findById("installation-1")).thenReturn(of(installation_found));
        when(installationRepositoryRepository.findById("unknown")).thenReturn(empty());
        when(installationRepositoryRepository.findById("installation-to-delete")).thenReturn(empty());
        when(installationRepositoryRepository.update(argThat(o -> o != null && o.getId().equals("unknown"))))
            .thenThrow(new TechnicalException());

        when(installationRepositoryRepository.update(argThat(o -> o != null && o.getId().equals("installation-1"))))
            .thenReturn(installation_updated);

        when(installationRepositoryRepository.update(argThat(o -> o == null || o.getId().equals("unknown"))))
            .thenThrow(new IllegalStateException());
    }
}
