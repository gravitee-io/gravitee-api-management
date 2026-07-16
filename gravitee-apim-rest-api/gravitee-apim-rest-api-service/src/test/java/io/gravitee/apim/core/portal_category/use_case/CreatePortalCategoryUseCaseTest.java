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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal_category.model.CreatePortalCategory;
import io.gravitee.apim.infra.domain_service.portal_category.PortalCategoryDomainServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePortalCategoryUseCaseTest {

    private static final String ENVIRONMENT_ID = "env-1";

    private PortalCategoryCrudServiceInMemory portalCategoryCrudServiceInMemory;
    private CreatePortalCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        portalCategoryCrudServiceInMemory = new PortalCategoryCrudServiceInMemory();
        var domainService = new PortalCategoryDomainServiceImpl(portalCategoryCrudServiceInMemory);
        useCase = new CreatePortalCategoryUseCase(domainService, portalCategoryCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        portalCategoryCrudServiceInMemory.reset();
    }

    @Test
    void should_create_portal_category() {
        var toCreate = CreatePortalCategory.builder().title("Weather").description("Weather APIs").visible(true).build();

        var output = useCase.execute(new CreatePortalCategoryUseCase.Input(ENVIRONMENT_ID, toCreate));

        assertThat(output.portalCategory().getId()).isNotNull();
        assertThat(output.portalCategory().getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(output.portalCategory().getTitle()).isEqualTo("Weather");
        assertThat(portalCategoryCrudServiceInMemory.storage()).hasSize(1);
    }

    @Test
    void should_throw_when_title_is_blank() {
        var toCreate = CreatePortalCategory.builder().title("").build();

        assertThatThrownBy(() -> useCase.execute(new CreatePortalCategoryUseCase.Input(ENVIRONMENT_ID, toCreate))).isInstanceOf(
            ValidationDomainException.class
        );
        assertThat(portalCategoryCrudServiceInMemory.storage()).isEmpty();
    }
}
