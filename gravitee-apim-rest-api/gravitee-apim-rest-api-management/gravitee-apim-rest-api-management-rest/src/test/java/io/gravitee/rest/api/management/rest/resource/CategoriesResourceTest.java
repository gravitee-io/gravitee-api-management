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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.NewCategoryEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Eric LELEU (eric dot leleu at graviteesource dot com)
 */
public class CategoriesResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/categories/";
    }

    @Inject
    private CategoryService categoryService;

    @Before
    public void init() {
        CategoryEntity cat1 = new CategoryEntity();
        cat1.setId("cat1-id");
        cat1.setName("cat1-name");
        cat1.setHidden(false);
        cat1.setOrder(2);

        CategoryEntity cat2 = new CategoryEntity();
        cat2.setId("cat2-id");
        cat2.setName("cat2-name");
        cat2.setHidden(false);
        cat2.setOrder(1);

        doReturn(List.of(cat1, cat2)).when(categoryService).findAll(GraviteeContext.getCurrentEnvironment());
    }

    @Test
    public void should_not_create_category_having_unsupported_picture() {
        var entity = NewCategoryEntity.builder()
            .name("My beautiful category")
            .description("my description")
            .picture("data:image/svg+xml;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
            .build();

        final Response response = envTarget().request().post(Entity.json(entity));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            soft
                .assertThat(response.readEntity(String.class))
                .contains("Invalid image format : Image mime-type image/svg+xml is not allowed");
        });
    }

    @Test
    public void should_not_create_category_having_unsupported_background() {
        var entity = NewCategoryEntity.builder()
            .name("My beautiful category")
            .description("my description")
            .background("data:image/svg+xml;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
            .build();

        final Response response = envTarget().request().post(Entity.json(entity));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            soft
                .assertThat(response.readEntity(String.class))
                .contains("Invalid image format : Image mime-type image/svg+xml is not allowed");
        });
    }

    @Test
    public void should_create_category() {
        var entity = NewCategoryEntity.builder()
            .name("My beautiful category")
            .description("my description")
            .picture("data:image/png;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
            .background("data:image/jpeg;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
            .build();

        when(categoryService.create(any(), eq(entity))).thenReturn(CategoryEntity.builder().id("123").build());

        final Response response = envTarget().request().post(Entity.json(entity));
        assertThat(response.getStatus()).isEqualTo(OK_200);
    }

    @Test
    public void should_list_all_categories_without_api_count() throws IOException {
        final Response response = envTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(OK_200);

        final List<CategoryEntity> categories = response.readEntity(new GenericType<>() {});
        assertThat(categories)
            .hasSize(2)
            .extracting(CategoryEntity::getId, CategoryEntity::getTotalApis)
            .containsExactly(tuple("cat2-id", 0L), tuple("cat1-id", 0L));
    }
}
