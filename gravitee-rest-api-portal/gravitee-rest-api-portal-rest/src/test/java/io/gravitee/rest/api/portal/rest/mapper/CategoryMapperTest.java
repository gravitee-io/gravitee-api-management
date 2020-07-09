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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.portal.rest.model.Category;
import io.gravitee.rest.api.portal.rest.model.CategoryLinks;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.UriBuilder;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryMapperTest {

    private static final String CATEGORY_DESCRIPTION = "my-category-description";
    private static final String CATEGORY_ID = "my-category-id";
    private static final String CATEGORY_HIGHLIGHT_API = "my-category-highlight-api";
    private static final String CATEGORY_KEY = "my-category-key";
    private static final String CATEGORY_NAME = "my-category-name";
    private static final String CATEGORY_PICTURE = "my-category-picture";
    private static final String CATEGORY_PICTURE_URL = "my-category-picture-url";
    private static final String CATEGORY_BACKGROUND = "my-category-background";
    private static final String CATEGORY_BACKGROUND_URL = "my-category-background-url";

    private static final String CATEGORY_BASE_URL = "http://foo/bar";

    @InjectMocks
    private CategoryMapper categoryMapper;
    
    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setCreatedAt(nowDate);
        categoryEntity.setDescription(CATEGORY_DESCRIPTION);
        categoryEntity.setHidden(true);
        categoryEntity.setHighlightApi(CATEGORY_HIGHLIGHT_API);
        categoryEntity.setId(CATEGORY_ID);
        categoryEntity.setKey(CATEGORY_KEY);
        categoryEntity.setName(CATEGORY_NAME);
        categoryEntity.setOrder(11);
        categoryEntity.setPicture(CATEGORY_PICTURE);
        categoryEntity.setPictureUrl(CATEGORY_PICTURE_URL);
        categoryEntity.setTotalApis(42);
        categoryEntity.setUpdatedAt(nowDate);
        categoryEntity.setPicture(CATEGORY_BACKGROUND);
        categoryEntity.setPictureUrl(CATEGORY_BACKGROUND_URL);
        
        //init
        Category category = categoryMapper.convert(categoryEntity, UriBuilder.fromPath(CATEGORY_BASE_URL));
        assertEquals(CATEGORY_DESCRIPTION, category.getDescription());
        assertEquals(CATEGORY_KEY, category.getId());
        assertEquals(CATEGORY_NAME, category.getName());
        assertEquals(11, category.getOrder().intValue());
        assertEquals(42, category.getTotalApis().longValue());
        CategoryLinks links = category.getLinks();
        assertNotNull(links);
        assertEquals(CATEGORY_BASE_URL+"/environments/DEFAULT/apis/"+CATEGORY_HIGHLIGHT_API, links.getHighlightedApi());
        assertEquals(CATEGORY_BASE_URL+"/environments/DEFAULT/categories/"+CATEGORY_ID+"/picture?" + nowDate.getTime(), links.getPicture());
        assertEquals(CATEGORY_BASE_URL+"/environments/DEFAULT/categories/"+CATEGORY_ID+"/background?" + nowDate.getTime(), links.getBackground());
        assertEquals(CATEGORY_BASE_URL+"/environments/DEFAULT/categories/"+CATEGORY_ID, links.getSelf());
    }
}
