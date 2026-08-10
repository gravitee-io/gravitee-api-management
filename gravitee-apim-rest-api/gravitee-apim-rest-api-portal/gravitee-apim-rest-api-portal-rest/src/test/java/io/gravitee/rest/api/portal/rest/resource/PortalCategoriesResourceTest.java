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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalCategoryQueryServiceInMemory;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.rest.api.portal.rest.model.PortalCategoriesResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalCategoriesResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Autowired
    private PortalCategoryQueryServiceInMemory portalCategoryQueryService;

    @Override
    protected String contextPath() {
        return "portal-categories";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalCategoryQueryService.reset();
    }

    @Test
    void should_return_only_visible_categories() {
        portalCategoryQueryService.initWith(
            List.of(
                PortalCategory.of(PortalCategoryId.random(), ENV_ID, "Weather", "Weather APIs", true),
                PortalCategory.of(PortalCategoryId.random(), ENV_ID, "Hidden", "Hidden category", false),
                PortalCategory.of(PortalCategoryId.random(), "other-env", "Banking", "Banking APIs", true)
            )
        );

        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        var result = response.readEntity(PortalCategoriesResponse.class);
        assertThat(result.getData()).extracting(io.gravitee.rest.api.portal.rest.model.PortalCategory::getTitle).containsExactly("Weather");
    }

    @Test
    void should_return_empty_list_when_no_visible_categories() {
        portalCategoryQueryService.initWith(
            List.of(PortalCategory.of(PortalCategoryId.random(), ENV_ID, "Hidden", "Hidden category", false))
        );

        Response response = target().request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        var result = response.readEntity(PortalCategoriesResponse.class);
        assertThat(result.getData()).isEmpty();
    }
}
