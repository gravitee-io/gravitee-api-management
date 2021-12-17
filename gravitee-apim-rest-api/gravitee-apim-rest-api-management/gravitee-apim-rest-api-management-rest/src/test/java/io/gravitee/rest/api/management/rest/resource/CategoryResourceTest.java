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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.UpdateCategoryEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Eric LELEU (eric dot leleu at graviteesource dot com)
 */
public class CategoryResourceTest extends AbstractResourceTest {

    private static final String CATEGORY = "my-category";
    private static final String UNKNOWN_API = "unknown";

    @Override
    protected String contextPath() {
        return "configuration/categories/";
    }

    private CategoryEntity mockCategory;
    private UpdateCategoryEntity updateCategoryEntity;

    @Before
    public void init() {
        mockCategory = new CategoryEntity();
        mockCategory.setId(CATEGORY);
        mockCategory.setName(CATEGORY);
        mockCategory.setUpdatedAt(new Date());
        doReturn(mockCategory).when(categoryService).findById(CATEGORY, GraviteeContext.getCurrentEnvironment());

        updateCategoryEntity = new UpdateCategoryEntity();
        updateCategoryEntity.setDescription("toto");
        updateCategoryEntity.setName(CATEGORY);

        doReturn(mockCategory).when(categoryService).update(eq(CATEGORY), any());
    }

    @Test
    public void shouldUpdateApi_ImageWithUpperCaseType_issue4086() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream("/images/4086_jpeg.b64");
        String picture = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        updateCategoryEntity.setPicture(picture);
        final Response response = envTarget(CATEGORY).request().put(Entity.json(updateCategoryEntity));

        assertEquals(response.readEntity(String.class), OK_200, response.getStatus());
    }
}
