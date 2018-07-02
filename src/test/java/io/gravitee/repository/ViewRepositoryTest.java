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
import io.gravitee.repository.management.model.View;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ViewRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/view-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<View> views = viewRepository.findAll();

        assertNotNull(views);
        assertEquals(3, views.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final View view = new View();
        view.setId("new-view");
        view.setName("View name");
        view.setDescription("Description for the new view");
        view.setCreatedAt(new Date(1486771200000L));
        view.setUpdatedAt(new Date(1486771200000L));
        view.setHidden(true);
        view.setOrder(1);
        view.setDefaultView(true);

        int nbViewsBeforeCreation = viewRepository.findAll().size();
        viewRepository.create(view);
        int nbViewsAfterCreation = viewRepository.findAll().size();

        Assert.assertEquals(nbViewsBeforeCreation + 1, nbViewsAfterCreation);

        Optional<View> optional = viewRepository.findById("new-view");
        Assert.assertTrue("View saved not found", optional.isPresent());

        final View viewSaved = optional.get();
        Assert.assertEquals("Invalid saved view name.", view.getName(), viewSaved.getName());
        Assert.assertEquals("Invalid view description.", view.getDescription(), viewSaved.getDescription());
        Assert.assertEquals("Invalid view createdAt.", view.getCreatedAt(), viewSaved.getCreatedAt());
        Assert.assertEquals("Invalid view updatedAt.", view.getUpdatedAt(), viewSaved.getUpdatedAt());
        Assert.assertEquals("Invalid view hidden.", view.isHidden(), viewSaved.isHidden());
        Assert.assertEquals("Invalid view order.", view.getOrder(), viewSaved.getOrder());
        Assert.assertEquals("Invalid view defaultView.", view.isDefaultView(), viewSaved.isDefaultView());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<View> optional = viewRepository.findById("products");
        Assert.assertTrue("View to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved view name.", "Products", optional.get().getName());

        final View view = optional.get();
        view.setName("New product");
        view.setDescription("New description");
        view.setOrder(10);
        view.setDefaultView(true);
        view.setHidden(true);
        view.setCreatedAt(new Date(1486771200000L));
        view.setUpdatedAt(new Date(1486771200000L));
        view.setHighlightApi("new Highlighted API");

        int nbViewsBeforeUpdate = viewRepository.findAll().size();
        viewRepository.update(view);
        int nbViewsAfterUpdate = viewRepository.findAll().size();

        Assert.assertEquals(nbViewsBeforeUpdate, nbViewsAfterUpdate);

        Optional<View> optionalUpdated = viewRepository.findById("products");
        Assert.assertTrue("View to update not found", optionalUpdated.isPresent());

        final View viewUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved view name.", view.getName(), viewUpdated.getName());
        Assert.assertEquals("Invalid view description.", view.getDescription(), viewUpdated.getDescription());
        Assert.assertEquals("Invalid view createdAt.", view.getCreatedAt(), viewUpdated.getCreatedAt());
        Assert.assertEquals("Invalid view updatedAt.", view.getUpdatedAt(), viewUpdated.getUpdatedAt());
        Assert.assertEquals("Invalid view hidden.", view.isHidden(), viewUpdated.isHidden());
        Assert.assertEquals("Invalid view order.", view.getOrder(), viewUpdated.getOrder());
        Assert.assertEquals("Invalid view defaultView.", view.isDefaultView(), viewUpdated.isDefaultView());
        Assert.assertEquals("Invalid view highlight API.", view.getHighlightApi(), viewUpdated.getHighlightApi());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbViewsBeforeDeletion = viewRepository.findAll().size();
        viewRepository.delete("international");
        int nbViewsAfterDeletion = viewRepository.findAll().size();

        Assert.assertEquals(nbViewsBeforeDeletion - 1, nbViewsAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownView() throws Exception {
        View unknownView = new View();
        unknownView.setId("unknown");
        viewRepository.update(unknownView);
        fail("An unknown view should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        viewRepository.update(null);
        fail("A null view should not be updated");
    }
}
