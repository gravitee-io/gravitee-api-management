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
package io.gravitee.repository.management;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.Entrypoint;
import io.gravitee.repository.management.model.EntrypointReferenceType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EntrypointRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/entrypoint-tests/";
    }

    @Test
    public void shouldFindAllByReference() throws Exception {
        final Set<Entrypoint> entrypoints = entrypointRepository.findByReference("DEFAULT", EntrypointReferenceType.ORGANIZATION);

        assertNotNull(entrypoints);
        assertEquals(3, entrypoints.size());
        assertEquals("HTTP", entrypoints.iterator().next().getTarget());

        // Verify environmentIds is loaded for the entrypoint that has it
        Optional<Entrypoint> withEnvIds = entrypoints
            .stream()
            .filter(e -> "fa29c012-a0d2-4721-a9c0-12a0d26721db".equals(e.getId()))
            .findFirst();
        assertTrue(withEnvIds.isPresent(), "Entrypoint with environmentIds not found");
        assertEquals("env1;env2", withEnvIds.get().getEnvironmentIds());

        // Verify referenceId and referenceType are correctly mapped from storage
        assertEquals("DEFAULT", withEnvIds.get().getReferenceId());
        assertEquals(EntrypointReferenceType.ORGANIZATION, withEnvIds.get().getReferenceType());

        // Verify environmentIds is null for entrypoints that don't have it
        Optional<Entrypoint> withoutEnvIds = entrypoints
            .stream()
            .filter(e -> "aaeddaec-0e94-4f49-adda-ec0e947f4965".equals(e.getId()))
            .findFirst();
        assertTrue(withoutEnvIds.isPresent(), "Entrypoint without environmentIds not found");
        assertNull(withoutEnvIds.get().getEnvironmentIds());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setId("new-entrypoint");
        entrypoint.setTarget("HTTP");
        entrypoint.setReferenceId("DEFAULT");
        entrypoint.setReferenceType(EntrypointReferenceType.ORGANIZATION);
        entrypoint.setValue("Entrypoint value");
        entrypoint.setTags("internal;product");
        entrypoint.setEnvironmentIds("env1;env3");

        int nbEntryPointsBeforeCreation = entrypointRepository.findByReference("DEFAULT", EntrypointReferenceType.ORGANIZATION).size();
        entrypointRepository.create(entrypoint);
        int nbEntryPointsAfterCreation = entrypointRepository.findByReference("DEFAULT", EntrypointReferenceType.ORGANIZATION).size();

        Assertions.assertEquals(nbEntryPointsBeforeCreation + 1, nbEntryPointsAfterCreation);

        Optional<Entrypoint> optional = entrypointRepository.findById("new-entrypoint");
        Assertions.assertTrue(optional.isPresent(), "Entrypoint saved not found");
        assertEquals(entrypoint, optional.get(), "Invalid saved entrypoint.");
        assertEquals("HTTP", optional.get().getTarget(), "Invalid saved target.");
        assertEquals("env1;env3", optional.get().getEnvironmentIds(), "Invalid saved environmentIds.");
    }

    @Test
    public void shouldCreateWithNullEnvironmentIds() throws Exception {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setId("new-entrypoint-no-env");
        entrypoint.setTarget("HTTP");
        entrypoint.setReferenceId("DEFAULT");
        entrypoint.setReferenceType(EntrypointReferenceType.ORGANIZATION);
        entrypoint.setValue("Entrypoint value no env");
        entrypoint.setTags("internal");

        entrypointRepository.create(entrypoint);

        Optional<Entrypoint> optional = entrypointRepository.findById("new-entrypoint-no-env");
        Assertions.assertTrue(optional.isPresent(), "Entrypoint saved not found");
        assertNull(optional.get().getEnvironmentIds(), "environmentIds should be null");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Entrypoint> optional = entrypointRepository.findById("fa29c012-a0d2-4721-a9c0-12a0d26721db");
        Assertions.assertTrue(optional.isPresent(), "EntryPoint to update not found");
        Assertions.assertEquals("https://public-api.company.com", optional.get().getValue(), "Invalid saved entrypoint value.");
        Assertions.assertEquals("env1;env2", optional.get().getEnvironmentIds(), "Invalid saved environmentIds.");

        final Entrypoint entrypoint = optional.get();
        entrypoint.setValue("New value");
        entrypoint.setReferenceId("DEFAULT");
        entrypoint.setReferenceType(EntrypointReferenceType.ORGANIZATION);
        entrypoint.setTags("New tags");
        entrypoint.setEnvironmentIds("env3");

        int nbEntryPointsBeforeUpdate = entrypointRepository.findByReference("DEFAULT", EntrypointReferenceType.ORGANIZATION).size();
        entrypointRepository.update(entrypoint);
        int nbEntryPointsAfterUpdate = entrypointRepository.findByReference("DEFAULT", EntrypointReferenceType.ORGANIZATION).size();

        Assertions.assertEquals(nbEntryPointsBeforeUpdate, nbEntryPointsAfterUpdate);

        Optional<Entrypoint> optionalUpdated = entrypointRepository.findById("fa29c012-a0d2-4721-a9c0-12a0d26721db");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Entrypoint saved not found");
        assertEquals(entrypoint, optionalUpdated.get(), "Invalid saved entrypoint.");
        assertEquals("New value", optionalUpdated.get().getValue(), "Invalid saved value.");
        assertEquals("New tags", optionalUpdated.get().getTags(), "Invalid saved tags.");
        assertEquals("env3", optionalUpdated.get().getEnvironmentIds(), "Invalid saved environmentIds.");
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbEntryPointsBeforeDeletion = entrypointRepository.findByReference("DEFAULT", EntrypointReferenceType.ORGANIZATION).size();
        entrypointRepository.delete("fa29c012-a0d2-4721-a9c0-12a0d26721db");
        int nbEntryPointsAfterDeletion = entrypointRepository.findByReference("DEFAULT", EntrypointReferenceType.ORGANIZATION).size();

        Assertions.assertEquals(nbEntryPointsBeforeDeletion - 1, nbEntryPointsAfterDeletion);
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final Set<Entrypoint> beforeDelete = entrypointRepository.findByReference("ToBeDeleted", EntrypointReferenceType.ORGANIZATION);

        final List<String> deleted = entrypointRepository.deleteByReferenceIdAndReferenceType(
            "ToBeDeleted",
            EntrypointReferenceType.ORGANIZATION
        );

        final Set<Entrypoint> afterDelete = entrypointRepository.findByReference("ToBeDeleted", EntrypointReferenceType.ORGANIZATION);

        assertNotNull(beforeDelete);
        assertEquals(2, beforeDelete.size());
        assertEquals(2, deleted.size());
        assertEquals(0, afterDelete.size());
    }

    @Test
    public void shouldNotUpdateUnknownEntryPoint() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Entrypoint unknownEntryPoint = new Entrypoint();
            unknownEntryPoint.setId("unknown");
            entrypointRepository.update(unknownEntryPoint);
            fail("An unknown entrypoint should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            entrypointRepository.update(null);
            fail("A null entrypoint should not be updated");
        });
    }
}
