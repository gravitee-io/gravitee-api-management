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
import io.gravitee.repository.management.model.Tag;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class TagRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/tag-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Tag> tags = tagRepository.findAll();

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

        int nbTagsBeforeCreation = tagRepository.findAll().size();
        tagRepository.create(tag);
        int nbTagsAfterCreation = tagRepository.findAll().size();

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

        int nbTagsBeforeUpdate = tagRepository.findAll().size();
        tagRepository.update(tag);
        int nbTagsAfterUpdate = tagRepository.findAll().size();

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
        int nbTagsBeforeDeletion = tagRepository.findAll().size();
        tagRepository.delete("international");
        int nbTagsAfterDeletion = tagRepository.findAll().size();

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
}
