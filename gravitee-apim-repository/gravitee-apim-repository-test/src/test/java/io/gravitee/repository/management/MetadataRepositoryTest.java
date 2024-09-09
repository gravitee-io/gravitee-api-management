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

import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.repository.management.model.MetadataReferenceType;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class MetadataRepositoryTest extends AbstractManagementRepositoryTest {

    private static final String ENV_ID = "DEFAULT";

    @Override
    protected String getTestCasesPath() {
        return "/data/metadata-tests/";
    }

    @Test
    public void shouldFindByReferenceType() throws Exception {
        final List<Metadata> metadataList = metadataRepository.findByReferenceType(MetadataReferenceType.APPLICATION);

        assertNotNull(metadataList);
        assertEquals(1, metadataList.size());
    }

    @Test
    public void shouldFindByReferenceTypeAndId() throws Exception {
        final List<Metadata> metadataList = metadataRepository.findByReferenceTypeAndReferenceId(MetadataReferenceType.API, "apiId");

        assertNotNull(metadataList);
        assertEquals(1, metadataList.size());
    }

    @Test
    public void shouldFindByKeyAndReferenceType() throws Exception {
        final List<Metadata> metadataList = metadataRepository.findByKeyAndReferenceType("string", MetadataReferenceType.API);

        assertNotNull(metadataList);
        assertEquals(1, metadataList.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Metadata metadata = new Metadata();
        metadata.setKey("new-metadata");
        metadata.setReferenceType(MetadataReferenceType.ENVIRONMENT);
        metadata.setReferenceId(ENV_ID);
        metadata.setName("Metadata name");
        metadata.setFormat(MetadataFormat.STRING);
        metadata.setValue("String");

        int nbMetadataListBeforeCreation = metadataRepository.findByReferenceType(MetadataReferenceType.ENVIRONMENT).size();
        metadataRepository.create(metadata);
        int nbMetadataListAfterCreation = metadataRepository.findByReferenceType(MetadataReferenceType.ENVIRONMENT).size();

        Assert.assertEquals(nbMetadataListBeforeCreation + 1, nbMetadataListAfterCreation);

        Optional<Metadata> optional = metadataRepository.findById("new-metadata", ENV_ID, MetadataReferenceType.ENVIRONMENT);
        Assert.assertTrue("Metadata saved not found", optional.isPresent());

        final Metadata metadataSaved = optional.get();
        Assert.assertEquals("Invalid saved metadata name.", metadata.getName(), metadataSaved.getName());
        Assert.assertEquals("Invalid metadata format.", metadata.getFormat(), metadataSaved.getFormat());
        Assert.assertEquals("Invalid metadata value.", metadata.getValue(), metadataSaved.getValue());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Metadata> optional = metadataRepository.findById("boolean", ENV_ID, MetadataReferenceType.ENVIRONMENT);
        Assert.assertTrue("Metadata to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved metadata name.", "Boolean", optional.get().getName());

        final Metadata metadata = optional.get();
        metadata.setName("New metadata");
        metadata.setFormat(MetadataFormat.URL);
        metadata.setValue("New value");

        int nbMetadataListBeforeUpdate = metadataRepository.findByReferenceType(MetadataReferenceType.ENVIRONMENT).size();
        metadataRepository.update(metadata);
        int nbMetadataListAfterUpdate = metadataRepository.findByReferenceType(MetadataReferenceType.ENVIRONMENT).size();

        Assert.assertEquals(nbMetadataListBeforeUpdate, nbMetadataListAfterUpdate);

        Optional<Metadata> optionalUpdated = metadataRepository.findById("boolean", ENV_ID, MetadataReferenceType.ENVIRONMENT);
        Assert.assertTrue("Metadata to update not found", optionalUpdated.isPresent());

        final Metadata metadataUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved metadata name.", "New metadata", metadataUpdated.getName());
        Assert.assertEquals("Invalid metadata value.", "New value", metadataUpdated.getValue());
        Assert.assertEquals("Invalid metadata format.", MetadataFormat.URL, metadataUpdated.getFormat());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbMetadataListBeforeDeletion = metadataRepository
            .findByReferenceTypeAndReferenceId(MetadataReferenceType.APPLICATION, "applicationId")
            .size();
        metadataRepository.delete("mail", "applicationId", MetadataReferenceType.APPLICATION);
        int nbMetadataListAfterDeletion = metadataRepository
            .findByReferenceTypeAndReferenceId(MetadataReferenceType.APPLICATION, "applicationId")
            .size();

        Assert.assertEquals(nbMetadataListBeforeDeletion - 1, nbMetadataListAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownMetadata() throws Exception {
        Metadata unknownMetadata = new Metadata();
        unknownMetadata.setKey("unknown");
        unknownMetadata.setReferenceId("unknown");
        unknownMetadata.setReferenceType(MetadataReferenceType.ENVIRONMENT);
        metadataRepository.update(unknownMetadata);
        fail("An unknown metadata should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        metadataRepository.update(null);
        fail("A null metadata should not be updated");
    }

    @Test
    public void should_delete_by_reference_type_and_reference_id() throws Exception {
        Assert.assertEquals(2, metadataRepository.findByReferenceTypeAndReferenceId(MetadataReferenceType.API, "api-delete").size());

        List<String> metadataIds = metadataRepository.deleteByReferenceIdAndReferenceType("api-delete", MetadataReferenceType.API);

        assertEquals(2, metadataIds.size());
        Assert.assertEquals(0, metadataRepository.findByReferenceTypeAndReferenceId(MetadataReferenceType.API, "api-delete").size());
    }

    @Test(expected = DuplicateKeyException.class)
    public void should_not_create_an_existing_key() throws Exception {
        metadataRepository.create(
            Metadata
                .builder()
                .key("boolean")
                .referenceId("DEFAULT")
                .referenceType(MetadataReferenceType.ENVIRONMENT)
                .format(MetadataFormat.BOOLEAN)
                .name("boolean")
                .build()
        );
    }
}
