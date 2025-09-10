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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageAdapterTest {

    @Test
    void should_convert_repository_page_to_core_page() {
        var repoPage = new io.gravitee.repository.management.model.PortalPage();
        repoPage.setId("123e4567-e89b-12d3-a456-426614174000");
        repoPage.setContent("markdown content");
        var corePage = PortalPageAdapter.INSTANCE.toEntity(repoPage);
        assertThat(corePage).isNotNull();
        assertThat(corePage.id().toString()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
        assertThat(corePage.pageContent().content()).isEqualTo("markdown content");
    }

    @Test
    void should_return_null_when_repository_page_is_null() {
        assertThat(PortalPageAdapter.INSTANCE.toEntity(null)).isNull();
    }
}
