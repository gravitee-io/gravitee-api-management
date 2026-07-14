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
package io.gravitee.apim.infra.domain_service.portal_category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PortalCategoryCrudServiceInMemory;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal_category.exception.PortalCategoryNotFoundException;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalCategoryDomainServiceImplTest {

    private static final String ENVIRONMENT_ID = "env-1";
    private static final PortalCategoryId PORTAL_CATEGORY_ID = PortalCategoryId.of("00000000-0000-0000-0000-0000000000b1");
    private static final PortalCategoryId UNKNOWN_ID = PortalCategoryId.of("00000000-0000-0000-0000-000000000bad");

    private PortalCategoryCrudServiceInMemory portalCategoryCrudServiceInMemory;
    private PortalCategoryDomainServiceImpl domainService;

    @BeforeEach
    void setUp() {
        portalCategoryCrudServiceInMemory = new PortalCategoryCrudServiceInMemory();
        domainService = new PortalCategoryDomainServiceImpl(portalCategoryCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        portalCategoryCrudServiceInMemory.reset();
    }

    @Test
    void validateTitle_accepts_a_non_blank_title() {
        assertThatNoException().isThrownBy(() -> domainService.validateTitle("Weather"));
    }

    @Test
    void validateTitle_throws_when_title_is_blank() {
        assertThatThrownBy(() -> domainService.validateTitle("   ")).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void validateTitle_throws_when_title_is_null() {
        assertThatThrownBy(() -> domainService.validateTitle(null)).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void findByIdAndEnvironmentId_returns_the_matching_portal_category() {
        portalCategoryCrudServiceInMemory.initWith(List.of(PortalCategory.of(PORTAL_CATEGORY_ID, ENVIRONMENT_ID, "Title", null, true)));

        var found = domainService.findByIdAndEnvironmentId(ENVIRONMENT_ID, PORTAL_CATEGORY_ID);

        assertThat(found.getId()).isEqualTo(PORTAL_CATEGORY_ID);
    }

    @Test
    void findByIdAndEnvironmentId_throws_not_found_when_id_is_unknown() {
        assertThatThrownBy(() -> domainService.findByIdAndEnvironmentId(ENVIRONMENT_ID, UNKNOWN_ID)).isInstanceOf(
            PortalCategoryNotFoundException.class
        );
    }

    @Test
    void findByIdAndEnvironmentId_throws_not_found_when_category_belongs_to_another_environment() {
        portalCategoryCrudServiceInMemory.initWith(List.of(PortalCategory.of(PORTAL_CATEGORY_ID, "other-env", "Title", null, true)));

        assertThatThrownBy(() -> domainService.findByIdAndEnvironmentId(ENVIRONMENT_ID, PORTAL_CATEGORY_ID)).isInstanceOf(
            PortalCategoryNotFoundException.class
        );
    }
}
