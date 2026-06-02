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
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.TagReferenceType;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        final Tag tagProduct = tags
            .stream()
            .filter(tag -> "1d114170-466d-4952-9141-70466de95213".equals(tag.getId()))
            .findAny()
            .get();
        assertEquals("Products", tagProduct.getName());
        assertEquals("Description for products tag", tagProduct.getDescription());
        assertEquals(asList("group1", "group2"), tagProduct.getRestrictedGroups());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Tag tag = new Tag();
        tag.setId(UUID.random().toString());
        tag.setKey("new-tag");
        tag.setName("Tag name");
        tag.setDescription("Description for the new tag");
        tag.setRestrictedGroups(asList("g1", "groupNew"));
        tag.setReferenceId("DEFAULT");
        tag.setReferenceType(TagReferenceType.ORGANIZATION);

        int nbTagsBeforeCreation = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();
        tagRepository.create(tag);
        int nbTagsAfterCreation = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();

        Assertions.assertEquals(nbTagsBeforeCreation + 1, nbTagsAfterCreation);

        Optional<Tag> optional = tagRepository.findById(tag.getId());
        Assertions.assertTrue(optional.isPresent(), "Tag saved not found");

        final Tag tagSaved = optional.get();
        Assertions.assertEquals(tag.getName(), tagSaved.getName(), "Invalid saved tag name.");
        Assertions.assertEquals(tag.getKey(), tagSaved.getKey(), "Invalid saved tag key.");
        Assertions.assertEquals(tag.getDescription(), tagSaved.getDescription(), "Invalid tag description.");
        Assertions.assertEquals(tag.getRestrictedGroups(), tagSaved.getRestrictedGroups(), "Invalid tag groups.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Tag> optional = tagRepository.findById("1d114170-466d-4952-9141-70466de95213");
        Assertions.assertTrue(optional.isPresent(), "Tag to update not found");
        Assertions.assertEquals("Products", optional.get().getName(), "Invalid saved tag name.");

        final Tag tag = optional.get();
        tag.setName("New product");
        tag.setDescription("New description");
        tag.setRestrictedGroups(singletonList("group"));

        int nbTagsBeforeUpdate = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();
        tagRepository.update(tag);
        int nbTagsAfterUpdate = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();

        Assertions.assertEquals(nbTagsBeforeUpdate, nbTagsAfterUpdate);

        Optional<Tag> optionalUpdated = tagRepository.findById("1d114170-466d-4952-9141-70466de95213");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Tag to update not found");

        final Tag tagUpdated = optionalUpdated.get();
        Assertions.assertEquals("New product", tagUpdated.getName(), "Invalid saved tag name.");
        Assertions.assertEquals("New description", tagUpdated.getDescription(), "Invalid tag description.");
        Assertions.assertEquals(singletonList("group"), tagUpdated.getRestrictedGroups(), "Invalid tag groups.");
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbTagsBeforeDeletion = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();
        tagRepository.delete("70237305-6f68-450e-a373-056f68750e50");
        int nbTagsAfterDeletion = tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION).size();

        Assertions.assertEquals(nbTagsBeforeDeletion - 1, nbTagsAfterDeletion);
    }

    @Test
    public void shouldNotUpdateUnknownTag() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Tag unknownTag = new Tag();
            unknownTag.setId("unknown");
            tagRepository.update(unknownTag);
            fail("An unknown tag should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            tagRepository.update(null);
            fail("A null tag should not be updated");
        });
    }

    @Test
    public void shouldFindByKeyAndReference() throws Exception {
        final Optional<Tag> tag = tagRepository.findByKeyAndReference("other", "OTHER", TagReferenceType.ORGANIZATION);

        assertTrue(tag.isPresent());
        assertEquals("Other", tag.get().getName());
        assertEquals("other", tag.get().getKey());
        assertEquals("Description for other tag", tag.get().getDescription());
    }

    @Test
    public void should_find_by_keys_and_reference_id_and_reference_type() throws Exception {
        final Set<Tag> tags = tagRepository.findByKeysAndReference(
            Set.of("international", "stores", "not-to-be-found"),
            "DEFAULT",
            TagReferenceType.ORGANIZATION
        );

        assertThat(tags)
            .hasSize(2)
            .anyMatch(
                tag ->
                    tag.getId().equals("70237305-6f68-450e-a373-056f68750e50") &&
                    tag.getName().equals("International") &&
                    tag.getKey().equals("international")
            );
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
