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
package io.gravitee.repository.management;

import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Installation;
import io.gravitee.repository.utils.DateUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InstallationRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/installation-tests/";
    }

    @Test
    public void shouldCreateInstallation() throws TechnicalException {
        Installation installation = new Installation();
        installation.setId("1");
        installation.setCreatedAt(new Date(1000000000000L));
        installation.setUpdatedAt(new Date(1100000000000L));
        Map<String, String> additionalInformation = new HashMap<>();
        additionalInformation.put("key1", "value1");
        additionalInformation.put("key2", "value2");
        installation.setAdditionalInformation(additionalInformation);

        Installation createdInstallation = installationRepository.create(installation);

        assertNotNull(createdInstallation);
        assertNotNull(createdInstallation.getId());
        assertEquals(installation.getId(), createdInstallation.getId());
        assertTrue(DateUtils.compareDate(installation.getCreatedAt(), createdInstallation.getCreatedAt()));
        assertTrue(DateUtils.compareDate(installation.getUpdatedAt(), createdInstallation.getUpdatedAt()));
        assertEquals(installation.getAdditionalInformation(), createdInstallation.getAdditionalInformation());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Installation> optInstallation = installationRepository.findById("installation-1");
        assertNotNull(optInstallation);
        assertTrue(optInstallation.isPresent());

        final Installation installation = optInstallation.get();
        assertEquals("installation-1", installation.getId());
        assertTrue(DateUtils.compareDate(new Date(1000000000000L), installation.getCreatedAt()));
        assertTrue(DateUtils.compareDate(new Date(1100000000000L), installation.getUpdatedAt()));

        final Map<String, String> additionalInformation = installation.getAdditionalInformation();
        assertNotNull(additionalInformation);
        assertEquals(2, additionalInformation.size());
        assertEquals("value1", additionalInformation.get("key1"));
        assertEquals("value2", additionalInformation.get("key2"));
    }

    @Test
    public void shouldNotFindByUnknownId() throws TechnicalException {
        Optional<Installation> optInstallation = installationRepository.findById("unknown");

        assertNotNull(optInstallation);
        assertFalse(optInstallation.isPresent());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Installation installation = new Installation();
        installation.setId("installation-1");
        installation.setCreatedAt(new Date(1000000000000L));
        installation.setUpdatedAt(new Date(1200000000000L));
        Map<String, String> additionalInformation = new HashMap<>();
        additionalInformation.put("key1", "value1");
        additionalInformation.put("key2", "value22");
        additionalInformation.put("key3", "value3");
        installation.setAdditionalInformation(additionalInformation);

        Installation updatedInstallation = installationRepository.update(installation);

        assertEquals(installation.getId(), updatedInstallation.getId());
        assertTrue(DateUtils.compareDate(installation.getCreatedAt(), updatedInstallation.getCreatedAt()));
        assertTrue(DateUtils.compareDate(installation.getUpdatedAt(), updatedInstallation.getUpdatedAt()));
        assertEquals(installation.getAdditionalInformation(), updatedInstallation.getAdditionalInformation());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        installationRepository.delete("installation-to-delete");
        Optional<Installation> optInstallation = installationRepository.findById("installation-to-delete");

        assertNotNull(optInstallation);
        assertFalse(optInstallation.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownInstallation() throws Exception {
        Installation unknownInstallation = new Installation();
        unknownInstallation.setId("unknown");
        installationRepository.update(unknownInstallation);
        fail("An unknown installation should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        installationRepository.update(null);
        fail("A null installation should not be updated");
    }
}
