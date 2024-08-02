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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.TagReferenceType;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class TagRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/tag-tests/";
    }

    @Test
    public void shouldFindByReference() throws Exception {
        final Set<Tag> tags = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION);

        assertNotNull(tags);
        assertEquals(3, tags.size());
        final Tag tagProduct = tags.stream().filter(tag -> "products".equals(tag.getId())).findAny().get();
        assertEquals("Products", tagProduct.getName());
        assertEquals("Description for products tag", tagProduct.getDescription());
        assertEquals(asList("group1", "group2"), tagProduct.getRestrictedGroups());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Tag tag = new Tag();
        tag.setId("new-tag");
        tag.setName("Tag name");
        tag.setDescription("Description for the new tag");
        tag.setRestrictedGroups(asList("g1", "groupNew"));
        tag.setReferenceId("DEFAULT");
        tag.setReferenceType(TagReferenceType.ORGANIZATION);

        int nbTagsBeforeCreation = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();
        tagRepository.create(tag);
        int nbTagsAfterCreation = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();

        Assert.assertEquals(nbTagsBeforeCreation + 1, nbTagsAfterCreation);

        Optional<Tag> optional = tagRepository.findById("new-tag");
        Assert.assertTrue("Tag saved not found", optional.isPresent());

        final Tag tagSaved = optional.get();
        Assert.assertEquals("Invalid saved tag name.", tag.getName(), tagSaved.getName());
        Assert.assertEquals("Invalid tag description.", tag.getDescription(), tagSaved.getDescription());
        Assert.assertEquals("Invalid tag groups.", tag.getRestrictedGroups(), tagSaved.getRestrictedGroups());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Tag> optional = tagRepository.findById("products");
        Assert.assertTrue("Tag to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved tag name.", "Products", optional.get().getName());

        final Tag tag = optional.get();
        tag.setName("New product");
        tag.setDescription("New description");
        tag.setRestrictedGroups(singletonList("group"));

        int nbTagsBeforeUpdate = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();
        tagRepository.update(tag);
        int nbTagsAfterUpdate = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();

        Assert.assertEquals(nbTagsBeforeUpdate, nbTagsAfterUpdate);

        Optional<Tag> optionalUpdated = tagRepository.findById("products");
        Assert.assertTrue("Tag to update not found", optionalUpdated.isPresent());

        final Tag tagUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved tag name.", "New product", tagUpdated.getName());
        Assert.assertEquals("Invalid tag description.", "New description", tagUpdated.getDescription());
        Assert.assertEquals("Invalid tag groups.", singletonList("group"), tagUpdated.getRestrictedGroups());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbTagsBeforeDeletion = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();
        tagRepository.delete("international");
        int nbTagsAfterDeletion = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();

        Assert.assertEquals(nbTagsBeforeDeletion - 1, nbTagsAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownTag() throws Exception {
        Tag unknownTag = new Tag();
        unknownTag.setId("unknown");
        tagRepository.update(unknownTag);
        fail("An unknown tag should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        tagRepository.update(null);
        fail("A null tag should not be updated");
    }

    @Test
    public void shouldFindByIdAndReference() throws Exception {
        final Optional<Tag> tag = tagRepository.findByIdAndReference("other", "OTHER", TagReferenceType.ORGANIZATION);

        assertTrue(tag.isPresent());
        assertEquals("Other", tag.get().getName());
        assertEquals("Description for other tag", tag.get().getDescription());
    }

    @Test
    public void should_find_by_ids_and_reference_id_and_reference_type() throws Exception {
        final Set<Tag> tags = tagRepository.findByIdsAndReference(
            Set.of("international", "stores", "not-to-be-found"),
            "DEFAULT",
            TagReferenceType.ORGANIZATION
        );

        assertThat(tags).hasSize(2).anyMatch(tag -> tag.getId().equals("international") && tag.getName().equals("International"));
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final var nbBeforeDeletion = tagRepository.findByReference("ToBeDeleted", TagReferenceType.ORGANIZATION).size();
        final var deleted = tagRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", TagReferenceType.ORGANIZATION).size();
        final var nbAfterDeletion = tagRepository.findByReference("ToBeDeleted", TagReferenceType.ORGANIZATION).size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted);
        assertEquals(0, nbAfterDeletion);
    }
}
