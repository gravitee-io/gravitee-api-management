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
package io.gravitee.apim.core.portal_category.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PortalCategoryCrudServiceInMemory;
import io.gravitee.apim.core.portal_category.exception.PortalCategoryNotFoundException;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_category.model.UpdatePortalCategory;
import io.gravitee.apim.infra.domain_service.portal_category.PortalCategoryDomainServiceImpl;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePortalCategoryUseCaseTest {

    private static final String ENVIRONMENT_ID = "env-1";
    private static final PortalCategoryId PORTAL_CATEGORY_ID = PortalCategoryId.of("00000000-0000-0000-0000-0000000000b1");
    private static final PortalCategoryId UNKNOWN_ID = PortalCategoryId.of("00000000-0000-0000-0000-000000000bad");

    private PortalCategoryCrudServiceInMemory portalCategoryCrudServiceInMemory;
    private UpdatePortalCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        portalCategoryCrudServiceInMemory = new PortalCategoryCrudServiceInMemory();
        var domainService = new PortalCategoryDomainServiceImpl(portalCategoryCrudServiceInMemory);
        useCase = new UpdatePortalCategoryUseCase(domainService, portalCategoryCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        portalCategoryCrudServiceInMemory.reset();
    }

    @Test
    void should_update_existing_portal_category() {
        portalCategoryCrudServiceInMemory.initWith(List.of(PortalCategory.of(PORTAL_CATEGORY_ID, ENVIRONMENT_ID, "Old Title", null, true)));

        var toUpdate = UpdatePortalCategory.builder().title("New Title").description("New description").visible(false).build();

        var output = useCase.execute(new UpdatePortalCategoryUseCase.Input(ENVIRONMENT_ID, PORTAL_CATEGORY_ID, toUpdate));

        assertThat(output.portalCategory().getTitle()).isEqualTo("New Title");
        assertThat(output.portalCategory().getDescription()).isEqualTo("New description");
        assertThat(output.portalCategory().isVisible()).isFalse();
    }

    @Test
    void should_throw_not_found_when_id_is_unknown() {
        var toUpdate = UpdatePortalCategory.builder().title("New Title").build();

        assertThatThrownBy(() -> useCase.execute(new UpdatePortalCategoryUseCase.Input(ENVIRONMENT_ID, UNKNOWN_ID, toUpdate))).isInstanceOf(
            PortalCategoryNotFoundException.class
        );
    }
}
