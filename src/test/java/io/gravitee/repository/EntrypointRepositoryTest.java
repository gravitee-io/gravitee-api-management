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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Entrypoint;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class EntrypointRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/entrypoint-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Entrypoint> entrypoints = entrypointRepository.findAll();

        assertNotNull(entrypoints);
        assertEquals(3, entrypoints.size());
    }

    @Test
    public void shouldFindAllByuEnvironment() throws Exception {
        final Set<Entrypoint> entrypoints = entrypointRepository.findAllByEnvironment("DEFAULT");

        assertNotNull(entrypoints);
        assertEquals(3, entrypoints.size());
    }
    
    @Test
    public void shouldCreate() throws Exception {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setId("new-entrypoint");
        entrypoint.setEnvironmentId("DEFAULT");
        entrypoint.setValue("Entrypoint value");
        entrypoint.setTags("internal;product");

        int nbEntryPointsBeforeCreation = entrypointRepository.findAll().size();
        entrypointRepository.create(entrypoint);
        int nbEntryPointsAfterCreation = entrypointRepository.findAll().size();

        Assert.assertEquals(nbEntryPointsBeforeCreation + 1, nbEntryPointsAfterCreation);

        Optional<Entrypoint> optional = entrypointRepository.findById("new-entrypoint");
        Assert.assertTrue("Entrypoint saved not found", optional.isPresent());
        assertEquals("Invalid saved entrypoint.", entrypoint, optional.get());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Entrypoint> optional = entrypointRepository.findById("fa29c012-a0d2-4721-a9c0-12a0d26721db");
        Assert.assertTrue("EntryPoint to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved entrypoint value.", "https://public-api.company.com", optional.get().getValue());

        final Entrypoint entrypoint = optional.get();
        entrypoint.setValue("New value");
        entrypoint.setEnvironmentId("new_DEFAULT");
        entrypoint.setTags("New tags");

        int nbEntryPointsBeforeUpdate = entrypointRepository.findAll().size();
        entrypointRepository.update(entrypoint);
        int nbEntryPointsAfterUpdate = entrypointRepository.findAll().size();

        Assert.assertEquals(nbEntryPointsBeforeUpdate, nbEntryPointsAfterUpdate);

        Optional<Entrypoint> optionalUpdated = entrypointRepository.findById("fa29c012-a0d2-4721-a9c0-12a0d26721db");
        Assert.assertTrue("Entrypoint saved not found", optionalUpdated.isPresent());
        assertEquals("Invalid saved entrypoint.", entrypoint, optionalUpdated.get());
        assertEquals("Invalid saved value.", "New value", optionalUpdated.get().getValue());
        assertEquals("Invalid saved tags.", "New tags", optionalUpdated.get().getTags());
        assertEquals("Invalid saved environment.", "new_DEFAULT", optionalUpdated.get().getEnvironmentId());
        
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbEntryPointsBeforeDeletion = entrypointRepository.findAll().size();
        entrypointRepository.delete("fa29c012-a0d2-4721-a9c0-12a0d26721db");
        int nbEntryPointsAfterDeletion = entrypointRepository.findAll().size();

        Assert.assertEquals(nbEntryPointsBeforeDeletion - 1, nbEntryPointsAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownEntryPoint() throws Exception {
        Entrypoint unknownEntryPoint = new Entrypoint();
        unknownEntryPoint.setId("unknown");
        entrypointRepository.update(unknownEntryPoint);
        fail("An unknown entrypoint should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        entrypointRepository.update(null);
        fail("A null entrypoint should not be updated");
    }
}
